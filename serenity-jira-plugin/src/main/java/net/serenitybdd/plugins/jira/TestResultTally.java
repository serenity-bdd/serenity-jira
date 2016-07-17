package net.serenitybdd.plugins.jira;

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

public class TestResultTally<T> {
    
    private final ConcurrentMap<String, List<T>> testOutcomesTally;

    public TestResultTally() {
        this.testOutcomesTally = Maps.newConcurrentMap();
    }

    public synchronized void recordResult(String issueNumber, T outcome) {
        getTestOutcomeListForIssue(issueNumber).add(outcome);

    }

    public List<T> getTestOutcomesForIssue(String issueNumber) {
       return ImmutableList.copyOf(getTestOutcomeListForIssue(issueNumber));

    }

    protected List<T> getTestOutcomeListForIssue(final String issueNumber) {
        List<T> resultTallyForIssue = testOutcomesTally.get(issueNumber);
        if (resultTallyForIssue == null) {
            testOutcomesTally.putIfAbsent(issueNumber, new Vector<T>());
        }
        return testOutcomesTally.get(issueNumber);
    }
    
    public TestResult getResultForIssue(final String issueNumber) {
        List<T> testOutcomesForThisIssue = testOutcomesTally.get(issueNumber);
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
