package net.serenitybdd.plugins.jira.model;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class CascadingSelectOption {
    private final String option;
    private final Optional<CascadingSelectOption> parentOption;
    private List<CascadingSelectOption> nestedOptions;

    private final static List<CascadingSelectOption> NO_CHILDREN = ImmutableList.of();

    public CascadingSelectOption(String option, CascadingSelectOption parentOption) {
        this(option, parentOption, NO_CHILDREN);
    }

    public CascadingSelectOption(String option, CascadingSelectOption parentOption, List<CascadingSelectOption> nestedOptions) {
        this.option = option;
        this.parentOption = Optional.fromNullable(parentOption);
        this.nestedOptions = nestedOptions;
    }

    public String getOption() {
        return option;
    }

    public Optional<CascadingSelectOption> getParentOption() {
        return parentOption;
    }

    public List<CascadingSelectOption> getNestedOptions() {
        return ImmutableList.copyOf(nestedOptions);
    }

    public void addChildren(List<CascadingSelectOption> nestedOptions) {
        this.nestedOptions = ImmutableList.copyOf(nestedOptions);
    }
}
