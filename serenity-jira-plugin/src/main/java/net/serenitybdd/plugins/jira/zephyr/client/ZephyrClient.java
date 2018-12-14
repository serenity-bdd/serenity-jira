package net.serenitybdd.plugins.jira.zephyr.client;

import com.google.gson.JsonObject;
import net.serenitybdd.plugins.jira.zephyr.ZephyrUpdater.Status;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrCycle;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrExecutionDetails;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface ZephyrClient {

	String getProjectKey(String projectName) throws IOException;

	public String getVersionID(final String versionName, final String projectId) throws IOException;

	public Map<String, ZephyrCycle> getListOfCycles(final String projectId, final String issueId,
			final String versionId, String cycleId) throws IOException;

	public ZephyrCycle getCycle(final String cycleId) throws IOException;

	public String createCycle(ZephyrCycle cycle) throws IOException;

	public boolean updateCycle(ZephyrCycle cycle) throws IOException;

	public Map<String, String> getIssueStatusIdsOfProject(final String projectId) throws IOException;

	public ZephyrExecutionDetails updateTestExecution(final String executionId, final Status status,
			final String comment) throws IOException;

	public ZephyrExecutionDetails getTestExecutionDetails(final String executionId) throws IOException;

	public ZephyrExecutionDetails createTestExecution(final String issueId, final String projectId,
			final String versionId, final String cycleId, final String assigneeType, final String assignee)
			throws IOException;

	public List<ZephyrExecutionDetails> getListOfTestExecutions(final String issueId, final String projectId,
			final String versionId, final String cycleId) throws IOException;

	public JsonObject deleteTestExecution(final List<String> executionIds) throws IOException;
}
