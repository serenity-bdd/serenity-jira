package net.serenitybdd.plugins.jira.requirements

import net.thucydides.core.statistics.service.ClasspathTagProviderService
import spock.lang.Specification

class WhenLoadingTagProviders extends Specification {

    def "Custom fields requirement provider can be loaded"() {
        given: "classpath tag provider service"
            def ClasspathTagProviderService service = new ClasspathTagProviderService();
        when: "loading available tag providers"
            def providers = service.getTagProviders();
        then: "JIRACustomFieldsRequirementsProvider should be loaded"
            providers.
                findAll ({provider -> provider instanceof JIRACustomFieldsRequirementsProvider}).
                size() == 1
    }
}
