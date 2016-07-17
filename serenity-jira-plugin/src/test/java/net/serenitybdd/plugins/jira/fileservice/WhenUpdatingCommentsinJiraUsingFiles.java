package net.serenitybdd.plugins.jira.fileservice;


import net.serenitybdd.plugins.jira.JiraListener;
import net.serenitybdd.plugins.jira.model.IssueTracker;
import net.serenitybdd.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.FileSystemUtils;
import net.thucydides.core.util.MockEnvironmentVariables;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.nio.file.Path;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

public class WhenUpdatingCommentsinJiraUsingFiles {

    @Mock
    IssueTracker issueTracker;

    private EnvironmentVariables environmentVariables;

    private ClasspathWorkflowLoader workflowLoader;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);

        environmentVariables = new MockEnvironmentVariables();
        environmentVariables.setProperty("jira.url", "http://my.jira.server");
        environmentVariables.setProperty("thucydides.public.url", "http://my.server/myproject/thucydides");
        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"true");
        environmentVariables.setProperty("build.id","2012-01-17_15-39-03");

        workflowLoader = new ClasspathWorkflowLoader(ClasspathWorkflowLoader.BUNDLED_WORKFLOW, environmentVariables);
    }

    @After
    public void resetPluginSpecificProperties() {
        System.clearProperty(JiraListener.SKIP_JIRA_UPDATES);
    }

    /*private TestOutcome newTestOutcome(String testMethod, TestResult testResult) {
        TestOutcome result = TestOutcome.forTest(testMethod, WhenUpdatingCommentsInJIRA.SampleTestSuite.class);
        TestStep step = new TestStep("a narrative description");
        step.setResult(testResult);
        result.recordStep(step);
        return result;
    } */

    private MockEnvironmentVariables prepareMockEnvironment() {
        MockEnvironmentVariables mockEnvironmentVariables = new MockEnvironmentVariables();
        mockEnvironmentVariables.setProperty("jira.project", "MYPROJECT");
        mockEnvironmentVariables.setProperty("jira.url", "http://my.jira.server");
        mockEnvironmentVariables.setProperty("thucydides.public.url", "http://my.server/myproject/thucydides");
        return mockEnvironmentVariables;
    }


    @Test
    public void when_a_test_with_a_referenced_issue_finishes_the_plugin_should_add_a_new_comment_for_this_issue() throws IOException {
        JiraFileServiceUpdater jiraUpdater = new JiraFileServiceUpdater(issueTracker, environmentVariables, workflowLoader);
        Path directory = FileSystemUtils.getResourceAsFile("/fileservice/sampletestsuite").toPath();
        jiraUpdater.loadOutcomesFromDirectory(directory.toAbsolutePath().toString(), "issue_123_should.*");
        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void should_not_add_the_project_prefix_to_the_issue_number_if_already_present() throws IOException{
        MockEnvironmentVariables mockEnvironmentVariables = prepareMockEnvironment();
        JiraFileServiceUpdater jiraUpdater = new JiraFileServiceUpdater(issueTracker, mockEnvironmentVariables, workflowLoader);
        Path directory = FileSystemUtils.getResourceAsFile("/fileservice/sampletestsuite").toPath();
        jiraUpdater.loadOutcomesFromDirectory(directory.toAbsolutePath().toString(),"issue_123_should.*");
        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void should_add_the_project_prefix_to_the_issue_number_if_not_already_present() throws IOException {
        MockEnvironmentVariables mockEnvironmentVariables = prepareMockEnvironment();
        JiraFileServiceUpdater jiraUpdater = new JiraFileServiceUpdater(issueTracker, mockEnvironmentVariables, workflowLoader);
        Path directory = FileSystemUtils.getResourceAsFile("/fileservice/sampletestsuitewithoutprefixes").toPath();
        jiraUpdater.loadOutcomesFromDirectory(directory.toAbsolutePath().toString());
        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void when_a_test_with_a_referenced_annotated_issue_finishes_the_plugin_should_add_a_new_comment_for_this_issue() throws IOException{
        JiraFileServiceUpdater jiraUpdater = new JiraFileServiceUpdater(issueTracker, environmentVariables, workflowLoader);
        Path directory = FileSystemUtils.getResourceAsFile("/fileservice/sampletestsuitewithissueannotation").toPath();
        jiraUpdater.loadOutcomesFromDirectory(directory.toAbsolutePath().toString(),"issue_123_should.*");
        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
    }

    @Test
    public void when_a_test_with_several_referenced_issues_finishes_the_plugin_should_add_a_new_comment_for_each_issue() throws IOException{

        JiraFileServiceUpdater jiraUpdater = new JiraFileServiceUpdater(issueTracker, environmentVariables, workflowLoader);
        Path directory = FileSystemUtils.getResourceAsFile("/fileservice/sampletestsuite").toPath();
        jiraUpdater.loadOutcomesFromDirectory(directory.toAbsolutePath().toString());
        verify(issueTracker).addComment(eq("MYPROJECT-123"), anyString());
        verify(issueTracker).addComment(eq("MYPROJECT-456"), anyString());
    }



}
