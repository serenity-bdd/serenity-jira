package net.serenitybdd.plugins.jira

import net.thucydides.core.model.TestOutcome
import net.thucydides.core.util.MockEnvironmentVariables
import net.serenitybdd.plugins.jira.requirements.JIRACustomFieldsRequirementsProvider
import net.serenitybdd.plugins.jira.service.JIRAConfiguration
import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration
import spock.lang.Specification

class WhenReadingVersionsFromJira extends Specification {


    JIRAConfiguration configuration
    def environmentVariables = new MockEnvironmentVariables()
    JIRACustomFieldsRequirementsProvider requirementsProvider

    def setup() {
        environmentVariables.setProperty('jira.url','https://wakaleo.atlassian.net')
        environmentVariables.setProperty('jira.username','bruce')
        environmentVariables.setProperty('jira.password','batm0bile')
        environmentVariables.setProperty('jira.project','DEMO')

        configuration = new SystemPropertiesJIRAConfiguration(environmentVariables)
        requirementsProvider = new JIRACustomFieldsRequirementsProvider(configuration, environmentVariables)
    }

    def "should find the release structure using a custom field"() {
        given:
            environmentVariables.setProperty("thucydides.use.customfield.releases","true")
            def requirementsProvider = new JIRACustomFieldsRequirementsProvider(configuration, environmentVariables)
        when:
            def releases = requirementsProvider.getReleases()
        then:
            releases.collect {it.name}.containsAll(["Release 1","Release 2","Release 3","Release 4"])
        and:
            releases[0].children.collect {it.name}.containsAll(["Sprint 3", "Sprint 4", "Sprint 5"])
    }

    def "should find the release for a given issue"() {
        given:
            environmentVariables.setProperty("thucydides.use.customfield.releases","true")
            def requirementsProvider = new JIRACustomFieldsRequirementsProvider(configuration, environmentVariables)
        and:
            def testOutcome = Mock(TestOutcome)
            testOutcome.getIssueKeys() >> ["DEMO-2"]
        when:
            def tags = requirementsProvider.getTagsFor(testOutcome);
        then:
            tags.collect {it.name }.containsAll(["Release 1","Sprint 2"])

    }

}
