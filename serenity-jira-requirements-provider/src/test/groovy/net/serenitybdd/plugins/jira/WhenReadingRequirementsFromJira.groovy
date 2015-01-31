package net.serenitybdd.plugins.jira

import net.thucydides.core.requirements.model.Requirement
import net.thucydides.core.util.MockEnvironmentVariables
import net.serenitybdd.plugins.jira.requirements.JIRARequirementsProvider
import net.serenitybdd.plugins.jira.service.JIRAConfiguration
import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration
import spock.lang.Specification

class WhenReadingRequirementsFromJira extends Specification {


    JIRAConfiguration configuration
    def environmentVariables = new MockEnvironmentVariables()
    def requirementsProvider

    def setup() {
        environmentVariables.setProperty('jira.url','https://wakaleo.atlassian.net')
        environmentVariables.setProperty('jira.username','bruce')
        environmentVariables.setProperty('jira.password','batm0bile')
        environmentVariables.setProperty('jira.project','TRAD')

        configuration = new SystemPropertiesJIRAConfiguration(environmentVariables)
        requirementsProvider = new JIRARequirementsProvider(configuration)
    }

    def "Requirements can be loaded from the Epic/Story JIRA card structure"() {
        given:
            def requirementsProvider = new JIRARequirementsProvider(configuration)
        when:
            def requirements = requirementsProvider.getRequirements();
        then:
            requirements.size() == 5
        and:
            totalNumberOf(requirements) == 27
    }

    def "Child requirements should have parents"() {
        given:
        def requirementsProvider = new JIRARequirementsProvider(configuration)
        when:
        def requirements = requirementsProvider.getRequirements();
        then:
        def parent = requirements.get(0)
        def firstChild = parent.getChildren().get(0)
        firstChild.parent == parent.name
    }

    def "Requirements can be loaded from a custom JIRA card structure"() {
        given:
        environmentVariables.setProperty("jira.root.issue.type","epic")
        environmentVariables.setProperty("jira.requirement.links","Epic Link, relates to")
        and:
        def requirementsProvider = new JIRARequirementsProvider(configuration, environmentVariables)
        when:
        def requirements = requirementsProvider.getRequirements();
        then:
        totalNumberOf(requirements) == 29
    }

    def totalNumberOf(List<Requirement> requirements) {
        int total = 0;
        requirements.each {
            total++;
            if (it.children) {
                total = total + totalNumberOf(it.children)
            }
        }
        return total
    }


}
