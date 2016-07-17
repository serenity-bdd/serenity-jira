package net.serenitybdd.plugins.jira.fileservice;




import com.google.common.collect.Lists;
import net.thucydides.core.model.TestOutcome;
import net.thucydides.core.model.TestResult;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Lightweight copy of a TestOutcome.
 */
class TestOutcomeSummary {

    private List<String> issues = Lists.newArrayList();
    private String title;
    private Path linkToTestReport;
    private TestResult testResult;
    private String reportName;
    private String name;

    TestOutcomeSummary(TestOutcome testOutcome,Path testReportLink) {
        this.issues.addAll(testOutcome.getIssues());
        this.title = testOutcome.getTitle();
        this.linkToTestReport = testReportLink;
        this.testResult = testOutcome.getResult();
        this.reportName = testOutcome.getReportName();
        this.name = testOutcome.getName();
    }

    public List<String> getIssues()
    {
        return issues;
    }

    public Path getLinkToTestReport() {
        return linkToTestReport;
    }

    public String getTitle(){ return title;}

    public TestResult getTestResult()
    {
        return testResult;
    }

    String getReportName(){
        return reportName;
    }

    public String getName() { return name; }
}
