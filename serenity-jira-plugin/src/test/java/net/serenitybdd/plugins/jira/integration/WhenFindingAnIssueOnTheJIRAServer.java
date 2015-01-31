package net.serenitybdd.plugins.jira.integration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class WhenFindingAnIssueOnTheJIRAServer {

    private static final String JIRA_WEBSERVICE_URL = "https://wakaleo.atlassian.net/rpc/soap/jirasoapservice-v2";

    private String issueKey;

    private IssueHarness testIssueHarness;

    @Before
    public void createTestIssue() throws Exception {

        testIssueHarness = new IssueHarness(JIRA_WEBSERVICE_URL);
//        issueKey = testIssueHarness.createTestIssue();
    }

    @After
    public void deleteTestIssue() throws Exception {
//        testIssueHarness.deleteTestIssues();
    }

    @Test
    public void should_be_able_to_find_a_project_by_key() throws Exception {
//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//
//        RemoteProject project = session.getJiraSoapService().getProjectByKey(token, "DEMO");
//        assertThat(project, is(not(nullValue())));
    }

    @Test
    public void should_be_able_to_find_an_issue_by_id() throws Exception {
//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//
//        RemoteIssue issue = session.getJiraSoapService().getIssue(token, issueKey);
//        assertThat(issue, is(not(nullValue())));
    }


    @Test
    public void should_be_able_to_find_an_issue_by_jql() throws Exception {
//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//
//        RemoteIssue[] issues = session.getJiraSoapService().getIssuesFromJqlSearch(token, "key=" + issueKey, 1000);
//        assertThat(issues.length, is(1));
    }

    @Test
    public void should_be_able_to_list_the_comments_in_an_issue() throws Exception {
//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//
//        RemoteComment newComment = new RemoteComment();
//        newComment.setBody("A new comment");
//        newComment.setAuthor("bruce");
//        session.getJiraSoapService().addComment(token, issueKey, newComment);
//
//        RemoteComment[] comments = session.getJiraSoapService().getComments(token, issueKey);
//        assertThat(comments.length, is(1));
    }

    @Test
    public void should_be_able_to_add_a_new_comment_to_an_issue() throws Exception {
//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//
//        RemoteComment newComment = new RemoteComment();
//        newComment.setBody("A new comment");
//        newComment.setAuthor("bruce");
//        session.getJiraSoapService().addComment(token, issueKey, newComment);
//
//        RemoteComment[] comments = session.getJiraSoapService().getComments(token, issueKey);
//        assertThat(comments.length, is(1));
    }

    @Test
    public void should_be_able_to_read_the_existing_comments_on_an_issue() throws Exception {

//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//
//        RemoteComment newComment = new RemoteComment();
//        newComment.setBody("A new comment");
//        newComment.setAuthor("bruce");
//        session.getJiraSoapService().addComment(token, issueKey, newComment);
//
//        RemoteComment newComment2 = new RemoteComment();
//        newComment2.setBody("Another new comment");
//        newComment2.setAuthor("bruce");
//        session.getJiraSoapService().addComment(token, issueKey, newComment2);
//
//
//        RemoteComment[] comments = session.getJiraSoapService().getComments(token, issueKey);
//        assertThat(comments.length, is(2));
    }

    @Test
    public void should_be_able_to_read_the_status_of_an_issue() throws Exception {

//        SOAPSession session = SOAPSession.openConnectionTo(new URL(JIRA_WEBSERVICE_URL))
//                .usingCredentials("bruce", "batm0bile");
//
//        String token = session.getAuthenticationToken();
//        String status = session.getJiraSoapService().getIssue(token, issueKey).getStatus();
//
//        assertThat(status, is("1"));
    }

}
