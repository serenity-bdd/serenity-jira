package net.serenitybdd.plugins.jira.zephyr;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import net.serenitybdd.plugins.jira.JiraStepListener;
import net.serenitybdd.plugins.jira.client.JerseyJiraClient;
import net.serenitybdd.plugins.jira.domain.IssueSummary;
import net.serenitybdd.plugins.jira.domain.Project;
import net.serenitybdd.plugins.jira.model.IssueTracker;
import net.serenitybdd.plugins.jira.service.JIRAConnection;
import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration;
import net.serenitybdd.plugins.jira.workflow.ClasspathWorkflowLoader;
import net.serenitybdd.plugins.jira.workflow.Workflow;
import net.serenitybdd.plugins.jira.workflow.WorkflowLoader;
import net.serenitybdd.plugins.jira.zephyr.client.ZephyrClient;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrCycle;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrExecutionDetails;
import net.thucydides.core.annotations.Feature;
import net.thucydides.core.annotations.Issue;
import net.thucydides.core.annotations.Issues;
import net.thucydides.core.annotations.Story;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestStep;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.MockEnvironmentVariables;

public class WhenUpdatingZephyrExecutionStatus {

    @Feature
    public static final class SampleFeature {
        public class SampleStory {}
        public class SampleStory2 {}
    }

    @Story(SampleFeature.SampleStory.class)
    static class SampleTestCase {

        @Issue("#MYPROJECT-123")
        public void issue_123_should_be_fixed_now() {}

        @Issues({"#MYPROJECT-123","#MYPROJECT-456"})
        public void issue_123_and_456_should_be_fixed_now() {}

        public void anotherTest() {}
    }

    @Story(SampleFeature.SampleStory2.class)
    static class SampleTestCase2 {

        @Issue("#MYPROJECT-789")
        public void issue_789_should_be_fixed_now() {}

        @Issues({"#MYPROJECT-333","#MYPROJECT-444"})
        public void issue_333_and_444_should_be_fixed_now() {}

        public void anotherTest() {}
    }

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
        environmentVariables = new MockEnvironmentVariables();
        
        environmentVariables.setProperty(SystemPropertiesJIRAConfiguration.JIRA_URL, "http://my.jira.server");
        environmentVariables.setProperty(SystemPropertiesJIRAConfiguration.JIRA_PROJECT, "MYPROJECT");
        environmentVariables.setProperty(SystemPropertiesJIRAConfiguration.JIRA_USERNAME, "user");
        environmentVariables.setProperty(SystemPropertiesJIRAConfiguration.JIRA_PASSWORD, "password");
        environmentVariables.setProperty(SystemPropertiesJIRAConfiguration.JIRA_WIKI_RENDERER, "true");
    }

    @Mock
    IssueTracker issueTracker;
    
    @Mock
    ZephyrClient zephyrClient;
    
    @Mock
    WorkflowLoader workflowLoader;

    EnvironmentVariables environmentVariables;
    
    @Test
    public void a_successful_test_should_update_exec_status_even_if_workflow_is_not_activated() throws IOException {
    	
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");
        Workflow workflow = Mockito.mock(Workflow.class);
        JIRAConnection conn = Mockito.mock(JIRAConnection.class);
        JerseyJiraClient jiraClient = Mockito.mock(JerseyJiraClient.class);
        
        when(workflowLoader.load()).thenReturn(workflow);
        when(workflow.isActive()).thenReturn(false);
        when(conn.getRestJiraClient()).thenReturn(jiraClient);
        when(jiraClient.getIssue(anyString())).thenReturn(new IssueSummary(null, 123L, null, null, null, null, "Test", null));
        when(jiraClient.getProjectByKey(anyString())).thenReturn(new Project("456", null, null, null, null, null, null));
        
        when(zephyrClient.createCycle(any(ZephyrCycle.class))).thenReturn("cycle1");
        when(zephyrClient.createTestExecution("123","456", null, "cycle1", null, null)).thenReturn(new ZephyrExecutionDetails("10"));
        when(zephyrClient.updateTestExecution(eq("10"),eq(ZephyrUpdater.Status.PASS), anyString())).thenReturn(new ZephyrExecutionDetails("10"));
        when(issueTracker.getJiraConnection()).thenReturn(conn);
        
        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"false");
        environmentVariables.setProperty(ZephyrUpdater.IS_ZEPHYR_ACTIVE,"true");
        
        JiraStepListener listener = new JiraStepListener(issueTracker, environmentVariables, workflowLoader, zephyrClient);

        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        verify(zephyrClient).updateTestExecution(eq("10"),eq(ZephyrUpdater.Status.PASS), anyString());
    }
    
    @Test
    public void a_successful_test_should_not_update_exec_status_when_zephyr_not_activated() throws IOException {
    	
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");
        Workflow workflow = Mockito.mock(Workflow.class);
       
        
        when(workflowLoader.load()).thenReturn(workflow);
        when(workflow.isActive()).thenReturn(false);
       
        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"false");
        environmentVariables.setProperty(ZephyrUpdater.IS_ZEPHYR_ACTIVE,"false");
        
        JiraStepListener listener = new JiraStepListener(issueTracker, environmentVariables, workflowLoader, zephyrClient);

        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        verify(zephyrClient, never()).updateTestExecution(anyString(),any(ZephyrUpdater.Status.class), anyString());
    }
    
    @Test
    public void a_successful_test_should_not_update_exec_status_when_zephyr_empty() throws IOException {
    	
        TestOutcome result = newTestOutcome("issue_123_should_be_fixed_now", TestResult.SUCCESS);

        when(issueTracker.getStatusFor("MYPROJECT-123")).thenReturn("Open");
        Workflow workflow = Mockito.mock(Workflow.class);
       
        
        when(workflowLoader.load()).thenReturn(workflow);
        when(workflow.isActive()).thenReturn(false);
       
        environmentVariables.setProperty(ClasspathWorkflowLoader.ACTIVATE_WORKFLOW_PROPERTY,"false");
        environmentVariables.setProperty(ZephyrUpdater.IS_ZEPHYR_ACTIVE,"");
        
        JiraStepListener listener = new JiraStepListener(issueTracker, environmentVariables, workflowLoader, zephyrClient);

        listener.testSuiteStarted(SampleTestCase.class);
        listener.testStarted("issue_123_should_be_fixed_now");
        listener.testFinished(result);
        listener.testSuiteFinished();
        verify(zephyrClient, never()).updateTestExecution(anyString(),any(ZephyrUpdater.Status.class), anyString());
    }

    private TestOutcome newTestOutcome(String testMethod, TestResult testResult) {
        TestOutcome result = TestOutcome.forTest(testMethod, SampleTestCase.class);
        TestStep step = new TestStep("a narrative description");
        step.setResult(testResult);
        result.recordStep(step);
        return result;
    }

    private TestOutcome newTestOutcomeForSampleClass2(String testMethod, TestResult testResult) {
        TestOutcome result = TestOutcome.forTest(testMethod, SampleTestCase2.class);
        TestStep step = new TestStep("a narrative description");
        step.setResult(testResult);
        result.recordStep(step);
        return result;
    }
}
