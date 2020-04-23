package net.serenitybdd.plugins.jira.domain;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.util.Optional;

public class Author {

    public static final String SELF_KEY = "self";
    public static final String NAME_KEY = "name";
    public static final String DISPLAY_NAME_KEY = "displayName";
    public static final String ACTIVE_KEY = "active";

    private String self;
    private String name;
    private String displayName;
    private boolean active;

    public Author(String self, String name, String displayName, boolean active) {
        this.self = self;
        this.name = name;
        this.displayName = displayName;
        this.active = active;
    }

    public static Author fromJsonString(String jsonIssueRepresentation) {
        JsonObject authorJson = new JsonParser().parse(jsonIssueRepresentation).getAsJsonObject();
        String self = authorJson.getAsJsonPrimitive(SELF_KEY).getAsString();
        String name = Optional.ofNullable(authorJson.getAsJsonPrimitive(NAME_KEY)).orElse(new JsonPrimitive("")).getAsString();
        String displayName = authorJson.getAsJsonPrimitive(DISPLAY_NAME_KEY).getAsString();
        boolean active = authorJson.getAsJsonPrimitive(ACTIVE_KEY).getAsBoolean();
        return new Author(self, name.isEmpty() ? displayName : name, displayName, active);
    }

    public String getSelf() {
        return self;
    }

    public void setSelf(String self) {
        this.self = self;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
