package net.serenitybdd.plugins.jira

import net.serenitybdd.plugins.jira.model.CascadingSelectOption
import net.serenitybdd.plugins.jira.requirements.ReleaseConverter
import spock.lang.Specification

class WhenExtractingReleasesFromCascadingSelects extends Specification {
    def releaseConverter = new ReleaseConverter()

    def "should convert a single option into a release"() {
        given:
            def selectOptions = [new CascadingSelectOption("Release 1", null)]
        when:
            def releases = releaseConverter.convertToReleases(selectOptions)
        then:
            releases.collect { it.name } == ["Release 1"]
    }


    def "should convert multiple option into a release list"() {
        given:
            def selectOptions = [new CascadingSelectOption("Release 1", null), new CascadingSelectOption("Release 2", null)]
        when:
            def releases = releaseConverter.convertToReleases(selectOptions)
        then:
            releases.collect { it.name } == ["Release 1", "Release 2"]
    }

    def "should convert nested options into a release list"() {
        given:
            def release1 = new CascadingSelectOption("Release 1", null)
            release1.addChildren([new CascadingSelectOption("Sprint 1", release1, []),
                                  new CascadingSelectOption("Sprint 2", release1, [])])
            def release2 = new CascadingSelectOption("Release 2", null)
            release2.addChildren([new CascadingSelectOption("Sprint 3", release2, []),
                                  new CascadingSelectOption("Sprint 4", release2, [])])

           def selectOptions = [release1, release2]
        when:
            def releases = releaseConverter.convertToReleases(selectOptions)
        then:
            releases.collect { it.name } == ["Release 1", "Release 2"]
        and:
            releases[0].children.collect { it.name } ==  ["Sprint 1", "Sprint 2"]
        and:
            releases[1].children.collect { it.name } ==  ["Sprint 3", "Sprint 4"]
    }

    def "should record parent paths in releases"() {
        given:
            def release1 = new CascadingSelectOption("Release 1", null)
            release1.addChildren([new CascadingSelectOption("Sprint 1", release1, []),
                    new CascadingSelectOption("Sprint 2", release1, [])])
            def release2 = new CascadingSelectOption("Release 2", null)
            release2.addChildren([new CascadingSelectOption("Sprint 3", release2, []),
                    new CascadingSelectOption("Sprint 4", release2, [])])

            def selectOptions = [release1, release2]
        when:
            def releases = releaseConverter.convertToReleases(selectOptions)
        then:
            releases[0].children[0].parents.collect { it.name } == ["Release 1"]

    }
}