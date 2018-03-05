package net.serenitybdd.plugins.jira

import net.thucydides.core.model.TestOutcome
import net.thucydides.core.model.TestTag
import net.thucydides.core.requirements.RequirementsTagProvider
import net.thucydides.core.requirements.model.Requirement
import net.thucydides.core.util.MockEnvironmentVariables
import net.serenitybdd.plugins.jira.requirements.JIRACustomFieldsRequirementsProvider
import net.serenitybdd.plugins.jira.service.JIRAConfiguration
import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration
import spock.lang.Specification

class WhenReadingCustomFieldRequirementsFromJIRA extends Specification {


    JIRAConfiguration configuration
    def environmentVariables = new MockEnvironmentVariables()
    RequirementsTagProvider requirementsProvider

    def setup() {
        environmentVariables.setProperty('jira.url','https://wakaleo.atlassian.net')
        environmentVariables.setProperty('jira.username','bruce')git
        environmentVariables.setProperty('jira.password','batm0bile')
        environmentVariables.setProperty('jira.project','DEMO')
        environmentVariables.setProperty('thucydides.requirement.types','capability,feature')

        configuration = new SystemPropertiesJIRAConfiguration(environmentVariables)
        requirementsProvider = new JIRACustomFieldsRequirementsProvider(configuration, environmentVariables)
    }

    def "should read requirement tree from JIRA"() {
        when:
            List<Requirement> requirements = requirementsProvider.getRequirements()
        then:
            requirements.collect { it.name }.contains("Grow Apples")
        and:
            requirements.collect { it.type } as Set == ["capability"] as Set
        and:
            def List<List<String>> check = requirements.collect {
                it.children.collect{it.name}
            }
            def values = []
            check.each {
                it.each {
                    values << it
                }
            }
            values.containsAll(["Grow red apples","Grow green apples"])
        and:
            requirements[0].children.collect { it.type }.containsAll(["feature", "feature"])
    }

    def "should get corresponding requirements from a test outcome "() {
        given:
            TestOutcome outcome = Mock(TestOutcome)
            outcome.issueKeys >> ["DEMO-8"]
        when:
            Optional<Requirement> requirement = requirementsProvider.getParentRequirementOf(outcome)
        then:
            requirement.isPresent()
        and:
            requirement.get().name == "Grow normal potatoes"
        and:
            requirement.get().type == "feature"
    }

    def "should not crash when having no parent requirement from a test outcome "() {
        given:
        TestOutcome outcome = Mock(TestOutcome)
        outcome.issueKeys >> ["DEMO-7"]
        when:
        Optional<Requirement> requirement = requirementsProvider.getParentRequirementOf(outcome)
        then:
        requirement.isPresent() == false
    }

    def "should get corresponding requirement from a test outcome"() {
        given:
            TestOutcome outcome = Mock(TestOutcome)
            outcome.issueKeys >> ["FH-11"]
        when:
            def requirement = requirementsProvider.getParentRequirementOf(outcome)
        then:
            requirement.isPresent();
            requirement.get().name == "Points from special offers"
    }

    def "should get corresponding requirement tags from a test outcome"() {
        given:
        TestOutcome outcome = Mock(TestOutcome)
        outcome.issueKeys >> ["FH-11"]
        when:
        def tags = requirementsProvider.getTagsFor(outcome)
        then:
        tags.contains(TestTag.withName("Earning Points/Points from special offers").andType("feature"))
        tags.contains(TestTag.withName("Earning points from special offers").andType("Story"))
    }

    def "should get all matching requirements set from a test outcome "() {
        given:
        TestOutcome outcome = Mock(TestOutcome)
        outcome.issueKeys >> ["DEMO-8"]
        when:
        List<Requirement> requirements = requirementsProvider.getAssociatedRequirements(outcome)
        then:
        requirements.collect { it.name }.containsAll(["Grow normal potatoes"])
    }

    def "associated tags should include all requirements"() {
        given:
            TestOutcome outcome = Mock(TestOutcome)
            outcome.issueKeys >> ["DEMO-8"]
        when:
            def tags = requirementsProvider.getTagsFor(outcome)
        then:
            tags.collect { it.name }.containsAll(["Grow Potatoes/Grow normal potatoes"])
    }

    def "should get corresponding requirements from a test tag "() {
        given:
            TestTag tag = TestTag.withName("Earning Points/Points from special offers").andType("feature")
        when:
            Optional<Requirement> requirement = requirementsProvider.getRequirementFor(tag)
        then:
            requirement.isPresent()
        and:
            requirement.get().name == "Points from special offers"
    }
}
