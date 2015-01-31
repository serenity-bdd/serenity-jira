package net.serenitybdd.plugins.jira.domain;

import com.google.common.base.Optional;

import java.util.Map;

public class RenderedView {
    private static final String RENDERED_DESCRIPTION_FIELD = "Description";

    private final Map<String, String> renderedFieldValues;

    public RenderedView(Map<String, String> renderedFieldValues) {
        this.renderedFieldValues = renderedFieldValues;
    }

    public String getDescription() {
        return renderedFieldValues.containsKey(RENDERED_DESCRIPTION_FIELD) ?
                renderedFieldValues.get(RENDERED_DESCRIPTION_FIELD) : renderedFieldValues.get(RENDERED_DESCRIPTION_FIELD.toLowerCase());

    }

    public boolean hasField(String field) {
        return renderedFieldValues.containsKey(field);
    }

    public Optional<String> customField(String field) {
        return Optional.fromNullable(renderedFieldValues.get(field));
    }
}
