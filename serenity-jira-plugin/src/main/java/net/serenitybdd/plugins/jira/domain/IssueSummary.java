package net.serenitybdd.plugins.jira.domain;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssueSummary {

    private final URI self;
    private final Long id;
    private final String key;
    private final String summary;
    private final String description;
    private final String type;
    private final String status;
    private final List<String> labels;
    private final List<String> fixVersions;
    private final Map<String, Object> customFieldValues;
    private final Map<String, String> renderedFieldValues;

    private final List<IssueComment> comments;

    private final static List<IssueComment> NO_COMMENTS = ImmutableList.of();

    public IssueSummary(URI self, Long id, String key, String summary, String description, Map<String, String> renderedFieldValues, String type, String status) {
        this(self, id, key, summary, description, renderedFieldValues, type, status,
                new ArrayList<String>(), new ArrayList<String>(), new HashMap<String, Object>(), NO_COMMENTS);
    }

    public IssueSummary(URI self, Long id, String key, String summary, String description, Map<String, String> renderedFieldValues,
                        String type, String status, List<String> labels, List<String> fixVersions, Map<String, Object> customFields,
                        List<IssueComment> comments) {
        this.self = self;
        this.id = id;
        this.key = key;
        this.summary = summary;
        this.description = description;
        this.renderedFieldValues = renderedFieldValues;
        this.type = type;
        this.status = status;
        this.labels = ImmutableList.copyOf(labels);
        this.fixVersions = ImmutableList.copyOf(fixVersions);
        this.customFieldValues = ImmutableMap.copyOf(customFields);
        this.comments = ImmutableList.copyOf(comments);
    }

    public URI getSelf() {
        return self;
    }

    public Long getId() {
        return id;
    }

    public String getKey() {
        return key;
    }

    public String getSummary() {
        return summary;
    }

    public String getDescription() {
        return description;
    }

    public String getType() {
        return type;
    }

    public List<IssueComment> getComments() {
        return comments;
    }

    public String getStatus() {
        return status;
    }

    public List<String> getLabels() {
        return labels;
    }

    public List<String> getFixVersions() {
        return fixVersions;
    }

    @Override
    public String toString() {
        return "IssueSummary{" +
                "key='" + key + '\'' +
                ", summary='" + summary + '\'' +
                '}';
    }

    public Optional<CustomFieldCast> customField(String fieldName) {
        if (customFieldValues.get(fieldName) == null) {
            return Optional.absent();
        } else {
            return Optional.of(new CustomFieldCast(customFieldValues.get(fieldName)));
        }
    }

    public RenderedView getRendered() {
        return new RenderedView(renderedFieldValues);
    }

}
