package net.serenitybdd.plugins.jira;

import ch.lambdaj.function.convert.Converter;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.inject.Inject;
import net.serenitybdd.plugins.jira.domain.IssueComment;
import net.serenitybdd.plugins.jira.guice.Injectors;
import net.serenitybdd.plugins.jira.model.IssueTracker;
import net.serenitybdd.plugins.jira.model.NamedTestResult;
import net.serenitybdd.plugins.jira.model.TestResultComment;
import net.serenitybdd.plugins.jira.service.JIRAConfiguration;
import net.serenitybdd.plugins.jira.service.NoSuchIssueException;
import net.serenitybdd.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.serenitybdd.plugins.jira.workflow.Workflow;
import net.serenitybdd.plugins.jira.workflow.WorkflowLoader;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.model.*;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import net.thucydides.core.util.EnvironmentVariables;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static ch.lambdaj.Lambda.convert;

/**
 * Updates JIRA issues referenced in a story with a link to the corresponding story report.
 */
public class JiraListener implements StepListener {

    private static final String BUILD_ID_PROPERTY = "build.id";
    public static final String SKIP_JIRA_UPDATES = "serenity.skip.jira.updates";
    private final IssueTracker issueTracker;

    private static final Logger LOGGER = LoggerFactory.getLogger(JiraListener.class);
    private final JIRAConfiguration configuration;
    private Workflow workflow;
    WorkflowLoader loader;

    private final EnvironmentVariables environmentVariables;
    private final String projectPrefix;

    private final TestResultTally<TestOutcome> resultTally;
    private Set<String> testSuiteIssues;

    static int DEFAULT_MAX_THREADS = 4;

    @Inject
    public JiraListener(IssueTracker issueTracker,
                        EnvironmentVariables environmentVariables,
                        WorkflowLoader loader) {
        this.issueTracker = issueTracker;
        this.environmentVariables = environmentVariables;
        this.projectPrefix = environmentVariables.getProperty(ThucydidesSystemProperty.JIRA_PROJECT.getPropertyName());
        configuration = Injectors.getInjector().getInstance(JIRAConfiguration.class);
        this.loader = loader;
        this.resultTally = new TestResultTally();
        this.testSuiteIssues = new HashSet<>();
        workflow = loader.load();

        executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(getMaxJobs()));

        logStatus(environmentVariables);

    }

    private int getMaxJobs() {
        return environmentVariables.getPropertyAsInteger("jira.max.threads",DEFAULT_MAX_THREADS);
    }

    private void logStatus(EnvironmentVariables environmentVariables) {
        String jiraUrl = environmentVariables.getProperty(ThucydidesSystemProperty.JIRA_URL.getPropertyName());
        String reportUrl = ThucydidesSystemProperty.THUCYDIDES_PUBLIC_URL.from(environmentVariables,"");
        LOGGER.debug("JIRA LISTENER STATUS");
        LOGGER.debug("JIRA URL: {} ", jiraUrl);
        LOGGER.debug("REPORT URL: {} ", reportUrl);
        LOGGER.debug("WORKFLOW ACTIVE: {} ", workflow.isActive());
    }

    protected boolean shouldUpdateIssues() {

        String jiraUrl = environmentVariables.getProperty(ThucydidesSystemProperty.JIRA_URL.getPropertyName());
        String reportUrl = ThucydidesSystemProperty.THUCYDIDES_PUBLIC_URL.from(environmentVariables,"");
        if (workflow.isActive()) {
            LOGGER.debug("WORKFLOW TRANSITIONS: {}", workflow.getTransitions());
        }

        return !(StringUtils.isEmpty(jiraUrl) || StringUtils.isEmpty(reportUrl));
    }

    protected boolean shouldUpdateWorkflow() {
        return Boolean.valueOf(environmentVariables.getProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY));
    }

    public JiraListener() {
        this(Injectors.getInjector().getInstance(IssueTracker.class),
                Injectors.getInjector().getProvider(EnvironmentVariables.class).get() ,
                Injectors.getInjector().getInstance(WorkflowLoader.class));
    }

    protected IssueTracker getIssueTracker() {
        return issueTracker;
    }

    protected Workflow getWorkflow() {
        return workflow;
    }

    public void testSuiteStarted(final Class<?> testCase) {
        testSuiteIssues.clear();
    }

    public void testSuiteStarted(final Story story) {
        testSuiteIssues.clear();
    }

    public void testStarted(final String testName) {
    }


    public void testFinished(TestOutcome result) {
        if (shouldUpdateIssues()) {
            List<String> issues = addPrefixesIfRequired(stripInitialHashesFrom(issueReferencesIn(result)));
            tallyResults(result, issues);
            testSuiteIssues.addAll(issues);
        }
    }

    public void testRetried() {
    }

    private void tallyResults(TestOutcome result, List<String> issues) {
        for(String issue : issues) {
            resultTally.recordResult(issue, result);
        }
    }

    public void testSuiteFinished() {

        if (shouldUpdateIssues()) {
            updateIssueStatus(testSuiteIssues);
        }
    }

    private final ListeningExecutorService executorService;
    private final AtomicInteger queueSize = new AtomicInteger(0);

    private void updateIssueStatus(Set<String> issues) {
        queueSize.set(issues.size());
        for(final String issue : issues) {
            final ListenableFuture<String> future = executorService.submit(new Callable<String>() {
                @Override
                public String call() throws Exception {
                    return issue;
                }
            });
            future.addListener(new Runnable() {
                @Override
                public void run() {                    logIssueTracking(issue);
                    if (!dryRun()) {
                        updateIssue(issue, resultTally.getTestOutcomesForIssue(issue));
                        queueSize.decrementAndGet();
                    }
                }
            }, MoreExecutors.newDirectExecutorService());
            future.addListener(new Runnable() {
                @Override
                public void run() {
                    queueSize.decrementAndGet();
                }
            }, executorService);

        }
        waitTillUpdatesDone(queueSize);
    }

    private void waitTillUpdatesDone(AtomicInteger counter) {
        while (counter.get() > 0) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
            }
        }
    }

    private List<String> issueReferencesIn(TestOutcome result) {
        return result.getIssues();
    }

    private void updateIssue(String issueId, List<TestOutcome> testOutcomes) {

        try {
            TestResultComment testResultComment = newOrUpdatedCommentFor(issueId, testOutcomes);
            if (getWorkflow().isActive() && shouldUpdateWorkflow()) {
                updateIssueStatusFor(issueId, testResultComment.getOverallResult());
            }
        } catch (NoSuchIssueException e) {
            LOGGER.error("No JIRA issue found with ID {}", issueId);
        }
    }

    private void updateIssueStatusFor(final String issueId, final TestResult testResult) {
        LOGGER.info("Updating status for issue {} with test result {}", issueId, testResult);
        String currentStatus = issueTracker.getStatusFor(issueId);

        LOGGER.info("Issue {} currently has status '{}'", issueId, currentStatus);

        List<String> transitions = getWorkflow().getTransitions().forTestResult(testResult).whenIssueIs(currentStatus);
        LOGGER.info("Found transitions {} for issue {}", transitions, issueId);

        for(String transition : transitions) {
            issueTracker.doTransition(issueId, transition);
        }
    }

    private List<NamedTestResult> namedTestResultsFrom(List<TestOutcome> testOutcomes) {
        return convert(testOutcomes, toNamedTestResults());
    }

    private Converter<TestOutcome, NamedTestResult> toNamedTestResults() {
        return new Converter<TestOutcome, NamedTestResult>() {

            public NamedTestResult convert(TestOutcome from) {
                return new NamedTestResult(from.getTitle(), from.getResult());
            }
        };
    }

    private TestResultComment newOrUpdatedCommentFor(final String issueId, List<TestOutcome> testOutcomes) {
        LOGGER.info("Updating comments for issue {}", issueId);
        LOGGER.info("WIKI Rendering activated: {}", isWikiRenderedActive());

        List<IssueComment> comments = issueTracker.getCommentsFor(issueId);
        IssueComment existingComment = findExistingSerenityCommentIn(comments);
        String testRunNumber = environmentVariables.getProperty(BUILD_ID_PROPERTY);
        TestResultComment testResultComment;
        List<NamedTestResult> newTestResults = namedTestResultsFrom(testOutcomes);
        if (existingComment == null) {
            testResultComment = TestResultComment.comment(isWikiRenderedActive())
                                                  .withResults(namedTestResultsFrom(testOutcomes))
                                                  .withReportUrl(linkToReport(testOutcomes))
                                                  .withTestRun(testRunNumber).asComment();

            issueTracker.addComment(issueId, testResultComment.asText());
        } else {
            testResultComment = TestResultComment.fromText(existingComment.getBody())
                                                         .withWikiRendering(isWikiRenderedActive())
                                                         .withUpdatedTestResults(newTestResults)
                                                         .withUpdatedReportUrl(linkToReport(testOutcomes))
                                                         .withUpdatedTestRunNumber(testRunNumber);

            IssueComment updatedComment = existingComment.withText(testResultComment.asText());
            issueTracker.updateComment(issueId,updatedComment);
        }
        return testResultComment;
    }

    private IssueComment findExistingSerenityCommentIn(List<IssueComment> comments) {
        for (IssueComment comment : comments) {
            if (comment.getBody().contains("Thucydides Test Results")) {
                return comment;
            }
        }
        return null;
    }

    private void logIssueTracking(final String issueId) {
        if (dryRun()) {
            LOGGER.info("--- DRY RUN ONLY: JIRA WILL NOT BE UPDATED ---");
        }
        LOGGER.info("Updating JIRA issue: " + issueId);
        LOGGER.info("JIRA server: " + issueTracker.toString());
    }

    private boolean dryRun() {
        return Boolean.valueOf(environmentVariables.getProperty(SKIP_JIRA_UPDATES));
    }

    private String linkToReport(List<TestOutcome> testOutcomes) {
        TestOutcome firstTestOutcome = testOutcomes.get(0);
        String reportUrl = ThucydidesSystemProperty.THUCYDIDES_PUBLIC_URL.from(environmentVariables,"");
        String reportName = firstTestOutcome.getReportName() + ".html";
        return formatTestResultsLink(reportUrl, reportName);
    }

    private String formatTestResultsLink(String reportUrl, String reportName) {
        return reportUrl + "/" + reportName;
    }

    private boolean isWikiRenderedActive() {
        return configuration.isWikiRenderedActive();
    }

    private List<String> addPrefixesIfRequired(final List<String> issueNumbers) {
        return convert(issueNumbers, toIssueNumbersWithPrefixes());
    }

    private Converter<String, String> toIssueNumbersWithPrefixes() {
        return new Converter<String, String>() {
            public String convert(String issueNumber) {
                if (StringUtils.isEmpty(projectPrefix)) {
                    return issueNumber;
                }
                if (issueNumber.startsWith(projectPrefix)) {
                    return issueNumber;
                }
                return projectPrefix + "-" + issueNumber;
            }
        };
    }

    private List<String> stripInitialHashesFrom(final List<String> issueNumbers) {
        return convert(issueNumbers, toIssueNumbersWithoutHashes());
    }

    private Converter<String, String> toIssueNumbersWithoutHashes() {
        return new Converter<String, String>() {
            public String convert(String issueNumber) {

                if (issueNumber.startsWith("#")) {
                    return issueNumber.substring(1);
                } else {
                    return issueNumber;
                }

            }
        };
    }

    public void stepStarted(ExecutedStepDescription executedStepDescription) {

    }

    public void skippedStepStarted(ExecutedStepDescription description) {
    }

    public void stepFailed(StepFailure stepFailure) {

    }

    public void lastStepFailed(StepFailure stepFailure) {
    }

    public void stepIgnored() {

    }

    public void stepIgnored(String s) {
    }

    public void stepPending() {

    }

    public void stepPending(String s) {
    }

    public void assumptionViolated(String s) {
    }

    public void stepFinished() {

    }

    public void testFailed(TestOutcome testOutcome, Throwable cause) {
    }


    public void testIgnored() {

    }

    @Override
    public void testSkipped() {

    }

    @Override
    public void testPending() {

    }

    @Override
    public void testIsManual() {

    }

    public void notifyScreenChange() {
    }

    public void useExamplesFrom(DataTable dataTable) {
    }

    @Override
    public void addNewExamplesFrom(DataTable dataTable) {
    }

    public void exampleStarted(Map<String, String> stringStringMap) {
    }

    public void exampleStarted() {
    }

    public void exampleFinished() {
    }

    public TestResultTally getTestResultTally(){
        return resultTally;
    }

    public Set<String> getTestSuiteIssues() {
        return testSuiteIssues;
    }
}
