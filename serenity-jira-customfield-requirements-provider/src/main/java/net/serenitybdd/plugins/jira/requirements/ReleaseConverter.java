package net.serenitybdd.plugins.jira.requirements;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import net.thucydides.core.model.Release;
import net.thucydides.core.reports.html.ReportNameProvider;
import net.serenitybdd.plugins.jira.model.CascadingSelectOption;

import java.util.List;


public class ReleaseConverter {

    public List<Release> convertToReleases(List<CascadingSelectOption> releaseOptions) {
        List<Release> releases = Lists.newArrayList();
        for(CascadingSelectOption option : releaseOptions) {
            String releaseName = option.getOption();
            Release release = Release.ofVersion(releaseName)
                                     .withReport(getReportNameProvider().forRelease(releaseName));
            List<Release> children = convertToReleases(option.getNestedOptions(), ImmutableList.of(release));
            releases.add(release.withChildren(children));
        }
        return releases;
    }

    private List<Release> convertToReleases(List<CascadingSelectOption> releaseOptions, List<Release> parents) {
        List<Release> releases = Lists.newArrayList();
        for(CascadingSelectOption option : releaseOptions) {
            String releaseName = option.getOption();
            Release childRelease = Release.ofVersion(releaseName)
                                          .withReport(getReportNameProvider().forRelease(releaseName))
                                          .withParents(parents);

            List<Release> parentReleases = concat(parents, childRelease);
            List<Release> children = convertToReleases(option.getNestedOptions(), parentReleases);
            releases.add(childRelease.withChildren(children));
        }
        return releases;
    }

    private List<Release> concat(List<Release> parents, Release childRelease) {
        List<Release> parentsAndChild = Lists.newArrayList(parents);
        parentsAndChild.add(childRelease);
        return parentsAndChild;
    }

    private ReportNameProvider getReportNameProvider() {
        return new ReportNameProvider();
    }
}
