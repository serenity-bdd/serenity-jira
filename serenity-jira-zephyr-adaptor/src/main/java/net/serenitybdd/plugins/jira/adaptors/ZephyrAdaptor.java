package net.serenitybdd.plugins.jira.adaptors;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.reports.adaptors.TestOutcomeAdaptor;
import net.thucydides.core.util.EnvironmentVariables;
import net.serenitybdd.plugins.jira.client.JerseyJiraClient;
import net.serenitybdd.plugins.jira.domain.IssueSummary;
import net.serenitybdd.plugins.jira.service.JIRAConfiguration;
import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Read manual test results from the JIRA Zephyr plugin.
 */
public class ZephyrAdaptor implements TestOutcomeAdaptor {

    private static final String ZEPHYR_REST_API = "rest/zephyr/1.0";

    private static final int DEFAULT_MAX_THREADS = 16;

    private static Map<String, TestResult> TEST_STATUS_MAP;
    {
        TEST_STATUS_MAP = Maps.newHashMap();
        TEST_STATUS_MAP.put("PASS", TestResult.SUCCESS);
        TEST_STATUS_MAP.put("FAIL", TestResult.FAILURE);
        TEST_STATUS_MAP.put("WIP", TestResult.PENDING);
        TEST_STATUS_MAP.put("BLOCKED", TestResult.SKIPPED);
        TEST_STATUS_MAP.put("UNEXECUTED", TestResult.IGNORED);
        TEST_STATUS_MAP.put("DESCOPED", TestResult.IGNORED);
    }

    private final JerseyJiraClient jiraClient;
    private final String jiraProject;
    private final EnvironmentVariables environmentVariables;
    private final ListeningExecutorService executorService;
    private final AtomicInteger queueSize;

    public ZephyrAdaptor() {
        this(new SystemPropertiesJIRAConfiguration(Injectors.getInjector().getInstance(EnvironmentVariables.class)));
    }

    public ZephyrAdaptor(JIRAConfiguration jiraConfiguration) {
        jiraProject = jiraConfiguration.getProject();
        jiraClient = new JerseyJiraClient(jiraConfiguration.getJiraUrl(),
                                          jiraConfiguration.getJiraUser(),
                                          jiraConfiguration.getJiraPassword(),
                                          jiraProject);
        environmentVariables = Injectors.getInjector().getInstance(EnvironmentVariables.class);
        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(getMaxJobs()));
        queueSize = new AtomicInteger(0);
    }

    private int getMaxJobs() {
        return environmentVariables.getPropertyAsInteger("jira.max.threads",DEFAULT_MAX_THREADS);
    }

    @Override
    public List<TestOutcome> loadOutcomes() throws IOException {
        try {
            List<IssueSummary> manualTests = jiraClient.findByJQL("type=Test and project=" + jiraProject);
            return extractTestOutcomesFrom(manualTests);
        } catch (JSONException e) {
            throw new IllegalArgumentException("Failed to load Zephyr manual tests", e);
        }
    }

    private List<TestOutcome> extractTestOutcomesFrom(List<IssueSummary> manualTests) throws JSONException {
        int threads = environmentVariables.getPropertyAsInteger("zephyr.thread.count", DEFAULT_MAX_THREADS);

        final List<TestOutcome> outcomes = Collections.synchronizedList(new ArrayList<TestOutcome>());

        for(final IssueSummary manualTest : manualTests) {
            final ListenableFuture<IssueSummary> future = executorService.submit(new Callable<IssueSummary>() {
                @Override
                public IssueSummary call() throws Exception {
                    return manualTest;
                }
            });
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    try {
                        queueSize.incrementAndGet();
                        IssueSummary issue = future.get();
                        TestExecutionRecord testExecutionRecord = getTestExecutionRecordFor(issue.getId());
                        if (!testExecutionRecord.isDescoped) {
                            outcomes.add(convert(issue));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (ExecutionException e) {
                        e.printStackTrace();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }, MoreExecutors.sameThreadExecutor());
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    queueSize.decrementAndGet();
                }
            }, executorService);
        }

        waitTillFinished();

        return outcomes;
    }

    private void waitTillFinished() {
        while (queueSize.get() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    public TestOutcome convert(IssueSummary issue) {

        try {
            List<IssueSummary> associatedIssues = getLabelsWithMatchingIssues(issue);
            TestOutcome outcome = TestOutcome.forTestInStory("Manual test - " + issue.getSummary() + " (" + issue.getKey() + ")",
                    storyFrom(associatedIssues));
            outcome.setDescription(issue.getRendered().getDescription());
            outcome = outcomeWithTagsForIssues(outcome, issue, associatedIssues);
            TestExecutionRecord testExecutionRecord = getTestExecutionRecordFor(issue.getId());

            outcome.clearStartTime();

            addTestStepsTo(outcome, testExecutionRecord, issue.getId());

            if (noStepsAreDefined(outcome)) {
                updateOverallTestOutcome(outcome, testExecutionRecord);
            }
            return outcome.asManualTest();
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private void updateOverallTestOutcome(TestOutcome outcome, TestExecutionRecord testExecutionRecord) {
        outcome.setAnnotatedResult(testExecutionRecord.testResult);
        if (testExecutionRecord.executionDate != null) {
            outcome.setStartTime(testExecutionRecord.executionDate);
        }
    }

    private boolean noStepsAreDefined(TestOutcome outcome) {
        return outcome.getTestSteps().isEmpty();
    }

    private void addTestStepsTo(TestOutcome outcome, TestExecutionRecord testExecutionRecord, Long id) throws JSONException {

        JSONArray stepObjects = getTestStepsForId(id);

        if (hasTestSteps(stepObjects)) {
            for (int i = 0; i < stepObjects.length(); i++) {
                JSONObject step = stepObjects.getJSONObject(i);
                outcome.recordStep(fromJson(step, testExecutionRecord));
                if (testExecutionRecord.executionDate != null) {
                    outcome.setStartTime(testExecutionRecord.executionDate);
                }
            }
        }
    }

    private JSONArray getTestStepsForId(Long id) throws JSONException {
        WebTarget target = jiraClient.buildWebTargetFor(ZEPHYR_REST_API + "/teststep/" + id);
        Response response = target.request().get();
        jiraClient.checkValid(response);

        String jsonResponse = response.readEntity(String.class);
        return new JSONArray(jsonResponse);
    }

    private boolean hasTestSteps(JSONArray stepObjects) {
        return stepObjects.length() > 0;
    }

    private TestStep fromJson(JSONObject step, TestExecutionRecord testExecutionRecord) throws JSONException {
        String stepDescription = step.getString("htmlStep");
        return TestStep.forStepCalled(stepDescription).withResult(testExecutionRecord.testResult);
    }

    class TestExecutionRecord {
        public final TestResult testResult;
        public final DateTime executionDate;
        public final boolean isDescoped;

        TestExecutionRecord(TestResult testResult, DateTime executionDate, boolean isDescoped) {
            this.testResult = testResult;
            this.executionDate = executionDate;
            this.isDescoped = isDescoped;
        }
    }

    private TestExecutionRecord getTestExecutionRecordFor(Long id) throws JSONException {
        WebTarget target = jiraClient.buildWebTargetFor(ZEPHYR_REST_API + "/schedule").queryParam("issueId", id);
        Response response = target.request().get();
        jiraClient.checkValid(response);

        String jsonResponse = response.readEntity(String.class);
        JSONObject testSchedule = new JSONObject(jsonResponse);
        JSONArray schedules = testSchedule.getJSONArray("schedules");
        JSONObject statusMap = testSchedule.getJSONObject("status");

        if (hasTestSteps(schedules)) {
            JSONObject latestSchedule = schedules.getJSONObject(0);
            String executionStatus = latestSchedule.getString("executionStatus");
            DateTime executionDate = executionDateFor(latestSchedule);
            boolean descoped = (executionStatus.equalsIgnoreCase("descoped"));
            return new TestExecutionRecord(getTestResultFrom(executionStatus, statusMap), executionDate, descoped);
        } else {
            return new TestExecutionRecord(TestResult.PENDING, null, false);
        }
    }

    private DateTime executionDateFor(JSONObject latestSchedule) throws JSONException {
        if (latestSchedule.has("executedOn")) {
            return parser().parse(latestSchedule.getString("executedOn"));
        } else {
            return null;
        }
    }

    private ZephyrDateParser parser() {
        return new ZephyrDateParser(new DateTime());
    }

    private TestResult getTestResultFrom(String executionStatus, JSONObject statusMap) throws JSONException {
        JSONObject status = statusMap.getJSONObject(executionStatus);
        if (status != null) {
            String statusName = status.getString("name");
            if (TEST_STATUS_MAP.containsKey(statusName)) {
                return TEST_STATUS_MAP.get(statusName);
            }
        }
        return TestResult.PENDING;
    }

    private TestOutcome outcomeWithTagsForIssues(TestOutcome outcome, IssueSummary issueCard, List<IssueSummary> associatedIssues) {

        List<String> issueKeys = Lists.newArrayList();
        for(IssueSummary issue : associatedIssues) {
            issueKeys.add(issue.getKey());
        }
        return outcome.withIssues(issueKeys);
    }

    private Story storyFrom(List<IssueSummary> associatedIssues) {
        Optional<Story> associatedStory = storyAssociatedByLabels(associatedIssues);
        return associatedStory.or(Story.called("Manual tests"));
    }

    private Optional<Story> storyAssociatedByLabels(List<IssueSummary> associatedIssues) {
        if (!associatedIssues.isEmpty()) {
            return Optional.of(Story.called(associatedIssues.get(0).getSummary()));
        }
        return Optional.absent();
    }

    private List<IssueSummary> getLabelsWithMatchingIssues(IssueSummary issue) throws JSONException {
        List<IssueSummary> matchingIssues = Lists.newArrayList();
        for(String label : issue.getLabels()) {
            matchingIssues.addAll(issueWithKey(label).asSet());
        }
        return ImmutableList.copyOf(matchingIssues);
    }

    private Map<String, Optional<IssueSummary>> issueSummaryCache = Maps.newConcurrentMap();

    private Optional<IssueSummary> issueWithKey(String key) throws JSONException {
        if (issueSummaryCache.containsKey(key)) {
            return issueSummaryCache.get(key);
        } else {
            Optional<IssueSummary> issueSummary = jiraClient.findByKey(key);
            issueSummaryCache.put(key, issueSummary);
            return issueSummary;
        }
    }


    @Override
    public List<TestOutcome> loadOutcomesFrom(File file) throws IOException {
        return loadOutcomes();
    }
}
