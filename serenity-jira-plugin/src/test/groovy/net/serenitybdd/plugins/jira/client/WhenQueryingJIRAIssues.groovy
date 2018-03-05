package net.serenitybdd.plugins.jira.client

import net.serenitybdd.plugins.jira.domain.IssueSummary
import spock.lang.Specification

class WhenQueryingJIRAIssues extends Specification {

    def "should be able to read the status of an issue"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO")
        when:
                Optional<IssueSummary> issue = jiraClient.findByKey("DEMO-3")
        then:
            issue.isPresent()
        and:
            issue.get().status == "Open"
    }


    def "should be able to read the comments of an issue"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO")
        when:
            Optional<IssueSummary> issue = jiraClient.findByKey("DEMO-3")
        then:
            issue.get().comments
        and:
            issue.get().comments[0].body == "Integration test comment"
    }

}
