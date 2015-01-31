package net.serenitybdd.plugins.jira.domain;

/**
 * A comment associated with a given issue.
 */
public class IssueComment {


    private final String self;
    private final Long id;
    private final String text;
    private final String author;

    public IssueComment(String self, Long id, String text, String author) {
        this.self = self;
        this.id = id;
        this.text = text;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public String getText() {
        return text;
    }

    public String getAuthor() {
        return author;
    }

    public String getSelf() {
        return self;
    }

    public IssueComment withText(String text) {
        return new IssueComment(self, id, text, author);
    }

    public IssueComment withAuthor(String author) {
        return new IssueComment(self, id, text, author);
    }
}
