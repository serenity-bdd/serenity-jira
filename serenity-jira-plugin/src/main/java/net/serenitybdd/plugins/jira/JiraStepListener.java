package net.serenitybdd.plugins.jira;

import com.google.inject.Inject;
import net.serenitybdd.plugins.jira.guice.Injectors;
import net.serenitybdd.plugins.jira.model.IssueTracker;
import net.serenitybdd.plugins.jira.workflow.WorkflowLoader;
import net.thucydides.core.model.DataTable;
import net.thucydides.core.model.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestOutcomeSummary;
import net.thucydides.core.steps.ExecutedStepDescription;
import net.thucydides.core.steps.StepFailure;
import net.thucydides.core.steps.StepListener;
import net.thucydides.core.util.EnvironmentVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Updates JIRA issues referenced in a story with a link to the corresponding story report.
 */
public class JiraStepListener implements StepListener {


    private static final Logger LOGGER = LoggerFactory.getLogger(JiraStepListener.class);

    private final TestResultTally<TestOutcomeSummary> resultTally;
    private Set<String> testSuiteIssues;
    private JiraUpdater jiraUpdater;

    @Inject
    public JiraStepListener(IssueTracker issueTracker,
                            EnvironmentVariables environmentVariables,
                            WorkflowLoader loader) {
        this.resultTally = new TestResultTally<TestOutcomeSummary>();
        this.testSuiteIssues = new HashSet<>();
        jiraUpdater = new JiraUpdater(issueTracker,environmentVariables,loader);
    }

    public JiraStepListener() {
        this(Injectors.getInjector().getInstance(IssueTracker.class),
                Injectors.getInjector().getProvider(EnvironmentVariables.class).get() ,
                Injectors.getInjector().getInstance(WorkflowLoader.class));
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
        if (jiraUpdater.shouldUpdateIssues()) {
            List<String> issues = jiraUpdater.getPrefixedIssuesWithoutHashes(new TestOutcomeSummary(result));
            tallyResults(new TestOutcomeSummary(result), issues);
            testSuiteIssues.addAll(issues);
        }
    }

    private void tallyResults(TestOutcomeSummary result, List<String> issues) {
        for(String issue : issues) {
            resultTally.recordResult(issue, result);
        }
    }

    public void testSuiteFinished() {
        if (jiraUpdater.shouldUpdateIssues()) {
            jiraUpdater.updateIssueStatus(testSuiteIssues, resultTally);
        }
    }

    public void testRetried() {}

    public void stepStarted(ExecutedStepDescription executedStepDescription) {}

    public void skippedStepStarted(ExecutedStepDescription description) {}

    public void stepFailed(StepFailure stepFailure) {}

    public void lastStepFailed(StepFailure stepFailure) {}

    public void stepIgnored() {}

    public void stepIgnored(String s) {}

    public void stepPending() {}

    public void stepPending(String s) {}

    public void assumptionViolated(String s) {}

    @Override
    public void testRunFinished() {
    }

    public void stepFinished() {}

    public void testFailed(TestOutcome testOutcome, Throwable cause) {}

    public void testIgnored() {}

    @Override
    public void testSkipped() {}

    @Override
    public void testPending() {}

    @Override
    public void testIsManual() {}

    public void notifyScreenChange() {}

    public void useExamplesFrom(DataTable dataTable) {}

    @Override
    public void addNewExamplesFrom(DataTable dataTable) {}

    public void exampleStarted(Map<String, String> stringStringMap) {}

    public void exampleStarted() {}

    public void exampleFinished() {}

    public TestResultTally getTestResultTally(){
        return resultTally;
    }

    public Set<String> getTestSuiteIssues() {
        return testSuiteIssues;
    }

    public JiraUpdater getJiraUpdater()
    {
        return jiraUpdater;
    }
}
