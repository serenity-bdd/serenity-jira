package net.serenitybdd.plugins.jira.model;

import ch.lambdaj.function.convert.Converter;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;

import java.util.List;

import static ch.lambdaj.Lambda.convert;

public class JIRACommentBuilder {
    private final boolean wikiRendering;
    private final String testRunNumber;
    private final String reportUrl;
    private final List<NamedTestResult> namedTestResults;

    private final static String NEW_LINE = System.getProperty("line.separator");

    public JIRACommentBuilder(boolean wikiRendering) {
        this(wikiRendering, null);
    }

    public JIRACommentBuilder(final boolean wikiRendering, final String reportUrl) {
        this(wikiRendering, reportUrl, null, null);
    }

    public JIRACommentBuilder(final boolean wikiRendering,
                              final String reportUrl,
                              final List<NamedTestResult> testOutcomes) {
        this(wikiRendering, testOutcomes, reportUrl, null);
    }


    public JIRACommentBuilder(final boolean wikiRendering,
                              final List<NamedTestResult> testOutcomes,
                              final String reportUrl,
                              final String testRunNumber) {
        this(wikiRendering, reportUrl, testOutcomes, testRunNumber);
    }



    private static List<NamedTestResult> namedTestResultsFrom(List<TestOutcome> testOutcomes) {
        return convert(testOutcomes, toNamedTestResults());
    }

    private static Converter<TestOutcome, NamedTestResult> toNamedTestResults() {
        return new Converter<TestOutcome, NamedTestResult>() {

            public NamedTestResult convert(TestOutcome from) {
                return new NamedTestResult(from.getTitle(), from.getResult());
            }
        };
    }

    public JIRACommentBuilder(boolean wikiRendering,
                              String reportUrl,
                              List<NamedTestResult> namedTestResults,
                              String testRunNumber) {
        this.reportUrl = reportUrl;
        this.namedTestResults = namedTestResults;
        this.testRunNumber = testRunNumber;
        this.wikiRendering = wikiRendering;
    }


    public String asText() {
        StringBuilder commentBuilder = new StringBuilder();
        addLine(commentBuilder, bold("Thucydides Test Results"));

        if (wikiRendering) {
            addLine(commentBuilder, "[Test report|" + reportUrl + "]");
        } else {
            addLine(commentBuilder, "Test Report: " + reportUrl);
        }
        if (testRunNumber != null) {
            addLine(commentBuilder, "Test Run: " + testRunNumber);
        }
        addLineForEachTest(commentBuilder);
        return commentBuilder.toString();
    }

    private String bold(String text) {
        return (wikiRendering) ? "*" + text + "*" : text;
    }

    private void addLineForEachTest(StringBuilder commentBuilder) {
        if (namedTestResults != null) {
            for (NamedTestResult testResult : namedTestResults) {

                addLine(commentBuilder, "  - " + testResult.getTestName() + ": " + testResult.getTestResult() + " "
                + resultIconFor(testResult.getTestResult()));
            }
        }
    }

    private String resultIconFor(TestResult testResult) {
        if (!wikiRendering) {
            return "";
        }

        switch (testResult) {
            case SUCCESS: return ": (/)";
            case FAILURE: return ": (x)";
            case ERROR: return ": (x)";
            case PENDING: return ": (!)";
            case SKIPPED: return ": (!)";
            case IGNORED: return ": (!)";
            default: return ": (?)";
        }
    }

    private void addLine(StringBuilder commentBuilder, final String line) {
        commentBuilder.append(line).append(NEW_LINE);
    }

    public JIRACommentBuilder withResults(final List<NamedTestResult> testOutcomes) {
        return new JIRACommentBuilder(this.wikiRendering, reportUrl, testOutcomes);
    }

    public JIRACommentBuilder withTestRun(final String testRunNumber) {
        return new JIRACommentBuilder(this.wikiRendering, this.reportUrl, this.namedTestResults, testRunNumber);
    }

    public JIRACommentBuilder withReportUrl(final String reportUrl) {
        return new JIRACommentBuilder(this.wikiRendering, reportUrl, this.namedTestResults, this.testRunNumber);
    }

    public JIRACommentBuilder withNamedResults(List<NamedTestResult> namedTestResults) {
        return new JIRACommentBuilder(this.wikiRendering, this.reportUrl, namedTestResults, this.testRunNumber);
    }

    public TestResultComment asComment() {
        return new TestResultComment(reportUrl, testRunNumber, namedTestResults, wikiRendering);
    }
}
