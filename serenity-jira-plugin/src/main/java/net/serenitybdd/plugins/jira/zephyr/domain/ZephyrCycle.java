package net.serenitybdd.plugins.jira.zephyr.domain;

import com.google.gson.annotations.SerializedName;

public class ZephyrCycle {

	private Integer totalExecutions;
	private Integer totalExecuted;
	private String clonedCycleId;
	
	private String started;
	private String ended;
	private String expand;
	private String projectKey;
	private String createdByDisplay;
	private String startDate;
	private String endDate;
	
	private String versionId;
	private String environment;
	private String build;
	private String createdBy;
	private String name;
	private String description;
	private String modifiedBy;
	private String projectId;
	private String id;
	
	@SerializedName("executionSummaries")
	private ZephyrExecutionSummaries executionSummaries;

	public Integer getTotalExecutions() {
		return totalExecutions;
	}

	public void setTotalExecutions(Integer totalExecutions) {
		this.totalExecutions = totalExecutions;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getTotalExecuted() {
		return totalExecuted;
	}

	public void setTotalExecuted(Integer totalExecuted) {
		this.totalExecuted = totalExecuted;
	}

	public String getStarted() {
		return started;
	}

	public void setStarted(String started) {
		this.started = started;
	}

	public String getExpand() {
		return expand;
	}

	public void setExpand(String expand) {
		this.expand = expand;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getEnvironment() {
		return environment;
	}

	public void setEnvironment(String environment) {
		this.environment = environment;
	}

	public String getBuild() {
		return build;
	}

	public void setBuild(String build) {
		this.build = build;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getEnded() {
		return ended;
	}

	public void setEnded(String ended) {
		this.ended = ended;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getCreatedByDisplay() {
		return createdByDisplay;
	}

	public void setCreatedByDisplay(String createdByDisplay) {
		this.createdByDisplay = createdByDisplay;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public ZephyrExecutionSummaries getExecutionSummaries() {
		return executionSummaries;
	}

	public void setExecutionSummaries(ZephyrExecutionSummaries executionSummaries) {
		this.executionSummaries = executionSummaries;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getClonedCycleId() {
		return clonedCycleId;
	}

	public void setClonedCycleId(String clonedCycleId) {
		this.clonedCycleId = clonedCycleId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ZephyrCycle [totalExecutions=");
		builder.append(totalExecutions);
		builder.append(", endDate=");
		builder.append(endDate);
		builder.append(", description=");
		builder.append(description);
		builder.append(", totalExecuted=");
		builder.append(totalExecuted);
		builder.append(", started=");
		builder.append(started);
		builder.append(", expand=");
		builder.append(expand);
		builder.append(", projectKey=");
		builder.append(projectKey);
		builder.append(", versionId=");
		builder.append(versionId);
		builder.append(", environment=");
		builder.append(environment);
		builder.append(", build=");
		builder.append(build);
		builder.append(", createdBy=");
		builder.append(createdBy);
		builder.append(", ended=");
		builder.append(ended);
		builder.append(", name=");
		builder.append(name);
		builder.append(", modifiedBy=");
		builder.append(modifiedBy);
		builder.append(", projectId=");
		builder.append(projectId);
		builder.append(", createdByDisplay=");
		builder.append(createdByDisplay);
		builder.append(", startDate=");
		builder.append(startDate);
		builder.append(", executionSummaries=");
		builder.append(executionSummaries);
		builder.append("]");
		return builder.toString();
	}
}
