package net.serenitybdd.plugins.jira.fileservice;


import ch.lambdaj.function.convert.Converter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestResultList;

import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentMap;

import static ch.lambdaj.Lambda.convert;

class TestResultSummaryTally {
    private final ConcurrentMap<String, List<TestOutcomeSummary>> testOutcomesTally;

    TestResultSummaryTally() {
        this.testOutcomesTally = Maps.newConcurrentMap();
    }

    synchronized void recordResult(String issueNumber, TestOutcomeSummary outcomeSummary) {
        getTestOutcomeListForIssue(issueNumber).add(outcomeSummary);

    }

    List<TestOutcomeSummary> getTestOutcomesForIssue(String issueNumber) {
        return ImmutableList.copyOf(getTestOutcomeListForIssue(issueNumber));

    }

    private List<TestOutcomeSummary> getTestOutcomeListForIssue(final String issueNumber) {
        List<TestOutcomeSummary> resultTallyForIssue = testOutcomesTally.get(issueNumber);
        if (resultTallyForIssue == null) {
            testOutcomesTally.putIfAbsent(issueNumber, new Vector<TestOutcomeSummary>());
        }
        return testOutcomesTally.get(issueNumber);
    }

    public TestResult getResultForIssue(final String issueNumber) {
        List<TestOutcomeSummary> testOutcomesForThisIssue = testOutcomesTally.get(issueNumber);
        return TestResultList.overallResultFrom(convert(testOutcomesForThisIssue, toTestResults()));
    }

    private Converter<TestOutcome, TestResult> toTestResults() {
        return new Converter<TestOutcome, TestResult>() {
            public TestResult convert(TestOutcome from) {
                return from.getResult();
            }
        };
    }

    public Set<String> getIssues() {
        return testOutcomesTally.keySet();
    }
}
