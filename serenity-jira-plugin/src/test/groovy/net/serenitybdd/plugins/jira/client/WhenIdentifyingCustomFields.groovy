package net.serenitybdd.plugins.jira.client

import net.serenitybdd.plugins.jira.model.CustomField
import spock.lang.Specification

class WhenIdentifyingCustomFields extends Specification {

    def "Should be able to read a cascading select custom field"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").
                usingCustomFields(["Requirements"])
        when:
            List<CustomField> customFields = jiraClient.customFields
        then:
            customFields.collect { it.name } == ["Requirements"]
    }

    def "should be able to read the values of a cascading select field"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").usingMetadataIssueType("Story");
        when:
            def options = jiraClient.findOptionsForCascadingSelect("Requirements");
        then:
            options.collect { it.option }.containsAll("Grow Apples", "Grow Potatoes", "Raise Chickens", "Raise Sheep")
        and:
            options[0].nestedOptions.collect { it.option } == ["Grow red apples", "Grow green apples"]
    }

    def "Cascading select options should store parent options"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").usingMetadataIssueType("Story");
        when:
            def options = jiraClient.findOptionsForCascadingSelect("Requirements");
        then:
            options[0].nestedOptions[0].parentOption.get() == options[0]
    }


    def "should be able to read custom field values as rendered HTML if available"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").
                             usingCustomFields(["Acceptance Criteria"])
        when:
            def issue = jiraClient.findByKey("DEMO-8").get()
        then:
            issue.customField("Acceptance Criteria").isPresent()
        and:
            issue.rendered.customField("Acceptance Criteria").get() == "<p>Grow <b>BIG</b> Potatoes</p>"
    }

    def "should be able to read custom field values in non-rendered format"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").
                    usingCustomFields(["Acceptance Criteria"])
        when:
            def issue = jiraClient.findByKey("DEMO-8").get()
        then:
            issue.customField("Acceptance Criteria").isPresent()
        and:
            issue.customField("Acceptance Criteria").get().value() == "Grow *BIG* Potatoes"
    }

    def "should be able to read custom field values of a given type"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").
                                                  usingCustomFields(["capability"])
        when:
            def issue = jiraClient.findByKey("DEMO-8").get()
        then:
            issue.customField("capability").isPresent()
        and:
            issue.customField("capability").get().asString() == "Grow Potatoes"
    }

    def "should be able to read field value lists"() {
        given:
            def jiraClient = new JerseyJiraClient("https://wakaleo.atlassian.net", "bruce", "batm0bile","DEMO").
                    usingCustomFields(["Requirements"])
        when:
            def issue = jiraClient.findByKey("DEMO-8").get()
        then:
            issue.customField("Requirements").isPresent()
        and:
            issue.customField("Requirements").get().asListOf(String) == ["Grow Potatoes", "Grow normal potatoes"]

    }

}
