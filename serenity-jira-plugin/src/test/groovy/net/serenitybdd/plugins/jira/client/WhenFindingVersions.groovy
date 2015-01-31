package net.serenitybdd.plugins.jira.client

import net.serenitybdd.plugins.jira.domain.Version
import spock.lang.Specification

class WhenFindingVersions extends Specification {

    def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net","bruce","batm0bile","DEMO")

    def "should load all known versions for a given project"() {
        when:
            List<Version> versions = jiraClient.findVersionsForProject('DEMO')
        then:
            versions.size() == 8
        and:
            versions.collect {it.name} == ['Iteration 1.1', 'Version 1.0', 'Iteration 1.2', 'Version 2.0', 'Iteration 2.1', 'Iteration 2.2', 'Sprint 2.3','Sprint 2.4']
    }


}
