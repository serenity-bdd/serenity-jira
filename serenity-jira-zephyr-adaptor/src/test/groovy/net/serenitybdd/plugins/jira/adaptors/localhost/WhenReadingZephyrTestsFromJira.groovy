package net.serenitybdd.plugins.jira.adaptors.localhost

import net.serenitybdd.plugins.jira.adaptors.ZephyrAdaptor
import net.serenitybdd.plugins.jira.adaptors.ZephyrDateParser
import net.thucydides.core.model.TestOutcome
import net.thucydides.core.model.TestResult
import net.thucydides.core.util.MockEnvironmentVariables
import net.serenitybdd.plugins.jira.service.JIRAConfiguration

import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import spock.lang.Specification

class WhenReadingZephyrTestsFromJira extends Specification {


    JIRAConfiguration configuration
    def environmentVariables = new MockEnvironmentVariables()
    def outcomes

    def setup() {
        environmentVariables.setProperty('jira.url','http://54.253.114.157:8082')
        environmentVariables.setProperty('jira.username','john.smart')
        environmentVariables.setProperty('jira.password','arv-uf-gis-bo-gl')
        environmentVariables.setProperty('jira.project','PAV')

        configuration = new SystemPropertiesJIRAConfiguration(environmentVariables)

        outcomes = new ZephyrAdaptor(configuration).loadOutcomes()
    }

    def "should read manual test results from Zephyr tests"() {
        given:
            def zephyrAdaptor = new ZephyrAdaptor(configuration)
        when:
            def outcomes = zephyrAdaptor.loadOutcomes()
        then:
            !outcomes.isEmpty()
        and:
            outcomes.each { assert it.isManual() }
    }

    def "should get the user story associated with the tests via a label with the user story key"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Testing some stuff'}
        then:
            sampleTest.descriptionText.isPresent()
        and:
            sampleTest.descriptionText.get().contains("Scenario Do some tests")
    }

    def "should record the description associated with the test"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Testing some stuff'}
        then:
            sampleTest.descriptionText.isPresent()
        and:
            sampleTest.descriptionText.get().contains("<h2>")
    }


    def "should get the latest test result for a manual test"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Testing some stuff'}
        then:
            sampleTest.isManual()
            sampleTest.result == TestResult.SUCCESS
            sampleTest.startTime != null

    }

    def "should load the test steps for a manual test"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Testing some stuff'}
        then:
            sampleTest.isManual()
            sampleTest.testSteps.size() == 2
        and:
            sampleTest.testSteps[0].description.contains("Do something")
            sampleTest.testSteps[1].description.contains("Do something else")
    }

    def "should record the execution time of a manual test"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Testing some stuff'}
        then:
            sampleTest.startTime == timeAt("28/Aug/13 11:03 AM")
    }

    def "should read test result for a test with no steps when a successful execution has been recorded"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Do even more tests'}
        then:
            sampleTest.result == TestResult.SUCCESS
    }

    def "should read test result for a test with steps when a successful execution has been recorded"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Testing some stuff'}
        then:
            sampleTest.getResult() == TestResult.SUCCESS
    }


    def "should find the parent story for a manual test"() {
        when:
            TestOutcome sampleTest = outcomes.find { it.title.contains 'Do even more tests'}
        then:
            sampleTest.getTags()
    }

    def "should understand Zephyr's quirky date formatting"() {
        given:
            def today = new DateTime(2013,1,1,0,0)
            def ZephyrDateParser parser = new ZephyrDateParser(today)
        when:
            def parsedDate = parser.parse(zephyrDate)
        then:
            parsedDate == expectedDate
        where:
            zephyrDate          | expectedDate
            "26/Jul/13 4:03 PM" | new DateTime(2013,7,26,16,3)
            "Today 9:13 AM"     | new DateTime(2013,1,1,9,13)
            "Yesterday 9:13 AM" | new DateTime(2012,12,31,9,13)
            "Sunday 9:13 AM"    | new DateTime(2012,12,30,9,13)
            "Saturday 9:13 AM"  | new DateTime(2012,12,29,9,13)
            "Friday 9:13 AM"    | new DateTime(2012,12,28,9,13)
            "Thursday 9:13 AM"  | new DateTime(2012,12,27,9,13)
            "Wednesday 9:13 AM"  | new DateTime(2012,12,26,9,13)
    }

    DateTime timeAt(String date) {
        return DateTime.parse(date, DateTimeFormat.forPattern("d/MMM/yy hh:mm a"))
    }
}
