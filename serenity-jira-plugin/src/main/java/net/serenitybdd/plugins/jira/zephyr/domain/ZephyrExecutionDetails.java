package net.serenitybdd.plugins.jira.zephyr.domain;

public class ZephyrExecutionDetails {
	
	private String id;
	private String orderId;
	private String executionStatus;
	private String comment;

	private String htmlComment;
	private String cycleId;
	private String cycleName;
	private String versionId;
	private String versionName;
	private String projectId;
	private String createdBy;
	private String modifiedBy;

	private String issueId;
	private String issueKey;
	private String summary;

	private String label;
	private String component;
	private String projectKey;

	private String executionDefectCount;
	private String stepDefectCount;
	private String totalDefectCount;
	
	private String executedOn;
	private String executedBy;
	private String executedByDisplay;
	
	private String issueDescription;
	
	public ZephyrExecutionDetails(String id) {
		super();
		this.id = id;
	}
	
	public ZephyrExecutionDetails() {}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getOrderId() {
		return orderId;
	}

	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	public String getExecutionStatus() {
		return executionStatus;
	}

	public void setExecutionStatus(String executionStatus) {
		this.executionStatus = executionStatus;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public String getHtmlComment() {
		return htmlComment;
	}

	public void setHtmlComment(String htmlComment) {
		this.htmlComment = htmlComment;
	}

	public String getCycleId() {
		return cycleId;
	}

	public void setCycleId(String cycleId) {
		this.cycleId = cycleId;
	}

	public String getCycleName() {
		return cycleName;
	}

	public void setCycleName(String cycleName) {
		this.cycleName = cycleName;
	}

	public String getVersionId() {
		return versionId;
	}

	public void setVersionId(String versionId) {
		this.versionId = versionId;
	}

	public String getVersionName() {
		return versionName;
	}

	public void setVersionName(String versionName) {
		this.versionName = versionName;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getModifiedBy() {
		return modifiedBy;
	}

	public void setModifiedBy(String modifiedBy) {
		this.modifiedBy = modifiedBy;
	}

	public String getIssueId() {
		return issueId;
	}

	public void setIssueId(String issueId) {
		this.issueId = issueId;
	}

	public String getIssueKey() {
		return issueKey;
	}

	public void setIssueKey(String issueKey) {
		this.issueKey = issueKey;
	}

	public String getSummary() {
		return summary;
	}

	public void setSummary(String summary) {
		this.summary = summary;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getProjectKey() {
		return projectKey;
	}

	public void setProjectKey(String projectKey) {
		this.projectKey = projectKey;
	}

	public String getExecutionDefectCount() {
		return executionDefectCount;
	}

	public void setExecutionDefectCount(String executionDefectCount) {
		this.executionDefectCount = executionDefectCount;
	}

	public String getStepDefectCount() {
		return stepDefectCount;
	}

	public void setStepDefectCount(String stepDefectCount) {
		this.stepDefectCount = stepDefectCount;
	}

	public String getTotalDefectCount() {
		return totalDefectCount;
	}

	public void setTotalDefectCount(String totalDefectCount) {
		this.totalDefectCount = totalDefectCount;
	}

	public String getExecutedOn() {
		return executedOn;
	}

	public void setExecutedOn(String executedOn) {
		this.executedOn = executedOn;
	}

	public String getExecutedBy() {
		return executedBy;
	}

	public void setExecutedBy(String executedBy) {
		this.executedBy = executedBy;
	}

	public String getExecutedByDisplay() {
		return executedByDisplay;
	}

	public void setExecutedByDisplay(String executedByDisplay) {
		this.executedByDisplay = executedByDisplay;
	}

	public String getIssueDescription() {
		return issueDescription;
	}

	public void setIssueDescription(String issueDescription) {
		this.issueDescription = issueDescription;
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("ZephyrExecutionDetails [id=");
		builder.append(id);
		builder.append(", orderId=");
		builder.append(orderId);
		builder.append(", executionStatus=");
		builder.append(executionStatus);
		builder.append(", comment=");
		builder.append(comment);
		builder.append(", htmlComment=");
		builder.append(htmlComment);
		builder.append(", cycleId=");
		builder.append(cycleId);
		builder.append(", cycleName=");
		builder.append(cycleName);
		builder.append(", versionId=");
		builder.append(versionId);
		builder.append(", versionName=");
		builder.append(versionName);
		builder.append(", projectId=");
		builder.append(projectId);
		builder.append(", createdBy=");
		builder.append(createdBy);
		builder.append(", modifiedBy=");
		builder.append(modifiedBy);
		builder.append(", issueId=");
		builder.append(issueId);
		builder.append(", issueKey=");
		builder.append(issueKey);
		builder.append(", summary=");
		builder.append(summary);
		builder.append(", label=");
		builder.append(label);
		builder.append(", component=");
		builder.append(component);
		builder.append(", projectKey=");
		builder.append(projectKey);
		builder.append(", executionDefectCount=");
		builder.append(executionDefectCount);
		builder.append(", stepDefectCount=");
		builder.append(stepDefectCount);
		builder.append(", totalDefectCount=");
		builder.append(totalDefectCount);
		builder.append(", executedOn=");
		builder.append(executedOn);
		builder.append(", executedBy=");
		builder.append(executedBy);
		builder.append(", executedByDisplay=");
		builder.append(executedByDisplay);
		builder.append(", issueDescription=");
		builder.append(issueDescription);
		builder.append("]");
		return builder.toString();
	}
	
}
