package net.serenitybdd.plugins.jira.adaptors;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.serenitybdd.plugins.jira.model.JQLException;
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

    private static final String ZEPHYR_REST_API = "rest/zapi/latest";

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
        } catch (JQLException e) {
            throw new IllegalArgumentException("Failed to load Zephyr manual tests", e);
        }
    }

    private List<TestOutcome> extractTestOutcomesFrom(List<IssueSummary> manualTests) throws JQLException {
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
                    } catch (JQLException e) {
                        e.printStackTrace();
                    }
                }
            }, MoreExecutors.newDirectExecutorService());
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    waitTillQueueNotEmpty();
                    queueSize.decrementAndGet();
                }
            }, executorService);
        }
        waitTillFinished();
        return outcomes;
    }

    private void waitTillQueueNotEmpty() {
        while (queueSize.get() == 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
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
        } catch (JQLException e) {
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

    private void addTestStepsTo(TestOutcome outcome, TestExecutionRecord testExecutionRecord, Long id) throws JQLException {

        JsonArray stepObjects = getTestStepsForId(id);

        if (hasTestSteps(stepObjects)) {
            for (int i = 0; i < stepObjects.size(); i++) {
                JsonElement step = stepObjects.get(i);
                outcome.recordStep(fromJson(step.getAsJsonObject(), testExecutionRecord));
                if (testExecutionRecord.executionDate != null) {
                    outcome.setStartTime(testExecutionRecord.executionDate);
                }
            }
        }
    }

    private JsonArray getTestStepsForId(Long id) throws JQLException {
        WebTarget target = jiraClient.buildWebTargetFor(ZEPHYR_REST_API + "/teststep/" + id);
        Response response = target.request().get();
        jiraClient.checkValid(response);
        String jsonResponse = response.readEntity(String.class);
        return new JsonParser().parse(jsonResponse).getAsJsonArray();
    }

    private boolean hasTestSteps(JsonArray stepObjects) {
        return stepObjects.size() > 0;
    }

    private TestStep fromJson(JsonObject step, TestExecutionRecord testExecutionRecord) throws JQLException {
        String stepDescription = step.get("htmlStep").getAsString();
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

    private TestExecutionRecord getTestExecutionRecordFor(Long id) throws JQLException {
        WebTarget target = jiraClient.buildWebTargetFor(ZEPHYR_REST_API + "/execution").queryParam("issueId", id);
        Response response = target.request().get();
        jiraClient.checkValid(response);
        String jsonResponse = response.readEntity(String.class);
        JsonObject testSchedule = new JsonParser().parse(jsonResponse).getAsJsonObject();
        JsonArray executions = testSchedule.get("executions").getAsJsonArray();
        JsonObject statusMap = testSchedule.get("status").getAsJsonObject();

        if (hasTestSteps(executions)) {
            JsonObject latestExecution = executions.get(0).getAsJsonObject();
            String executionStatus = latestExecution.get("executionStatus").getAsString();
            DateTime executionDate = executionDateFor(latestExecution);
            boolean descoped = (executionStatus.equalsIgnoreCase("descoped"));
            return new TestExecutionRecord(getTestResultFrom(executionStatus, statusMap), executionDate, descoped);
        } else {
            return new TestExecutionRecord(TestResult.PENDING, null, false);
        }
    }

    private DateTime executionDateFor(JsonObject latestSchedule) throws JQLException {
        if (latestSchedule.has("executedOn")) {
            return parser().parse(latestSchedule.get("executedOn").getAsString());
        } else {
            return null;
        }
    }

    private ZephyrDateParser parser() {
        return new ZephyrDateParser(new DateTime());
    }

    private TestResult getTestResultFrom(String executionStatus, JsonObject statusMap) throws JQLException {
        JsonObject status = statusMap.get(executionStatus).getAsJsonObject();
        if (status != null) {
            String statusName = status.get("name").getAsString();
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

    private List<IssueSummary> getLabelsWithMatchingIssues(IssueSummary issue) throws JQLException {
        List<IssueSummary> matchingIssues = Lists.newArrayList();
        for(String label : issue.getLabels()) {
            matchingIssues.addAll(issueWithKey(label).asSet());
        }
        return ImmutableList.copyOf(matchingIssues);
    }

    private Map<String, Optional<IssueSummary>> issueSummaryCache = Maps.newConcurrentMap();

    private Optional<IssueSummary> issueWithKey(String key) throws JQLException {
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
