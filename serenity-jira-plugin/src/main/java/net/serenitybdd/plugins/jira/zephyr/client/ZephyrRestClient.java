package net.serenitybdd.plugins.jira.zephyr.client;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import net.serenitybdd.plugins.jira.service.JIRAConfiguration;
import net.serenitybdd.plugins.jira.service.JIRAConnection;
import net.serenitybdd.plugins.jira.service.JiraIssueTracker;
import net.serenitybdd.plugins.jira.zephyr.ZephyrDateParser;
import net.serenitybdd.plugins.jira.zephyr.ZephyrUpdater;
import net.serenitybdd.plugins.jira.zephyr.ZephyrUpdater.Status;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrCycle;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrExecutionDetails;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ZephyrRestClient implements ZephyrClient {

	private String zapiUrl;
	private String creds;
	private final Logger logger;

	private static final String ZAPI_LABEL = "label";
	private static final String ZAPI_VALUE = "value";
	
	private static final int MIN_TEST_SCENARIO_DELAY = 1800000;
	
	@Inject
	public ZephyrRestClient(JIRAConfiguration jiraConfiguration) {
		this(LoggerFactory.getLogger(JiraIssueTracker.class), jiraConfiguration);
	}

    public ZephyrRestClient(Logger logger, JIRAConfiguration jiraConfiguration) {
        this.logger = logger;
        JIRAConnection jiraConnection = new JIRAConnection(jiraConfiguration);
        this.zapiUrl = jiraConnection.getJiraWebserviceUrl() + "rest/zapi/latest/";
		this.creds = jiraConnection.getJiraUser() + ":" + jiraConnection.getJiraPassword();
    }

	/**
	 * Gets the the project key
	 * 
	 * @throws IOException
	 * @return the ID for the specified Version in the specified Project
	 */
    @Override
	public String getProjectKey(String projectName) throws IOException {
		// Get list of versions on the specified project
		final JsonObject projectJsonObj = HttpClient.httpGetJSONObject(this.zapiUrl + "util/project-list", creds);
		if (null == projectJsonObj) {
			throw new IllegalStateException("JSONObject is null for getAllProjects");
		}
		final JsonArray projectOptions = (JsonArray) projectJsonObj.get("options");

		// Iterate over projects
		for (int i = 0; i < projectOptions.size(); i++) {
			final JsonObject obj2 = projectOptions.get(i).getAsJsonObject();
			// If label matches specified version name
			if (obj2.get(ZAPI_LABEL).getAsString().equals(projectName)) {
				// Return the ID for this version
				return obj2.get(ZAPI_VALUE).getAsString();
			}
		}

		throw new IllegalStateException("Project " + projectName + " not found!!");
	}

	/**
	 * Gets the versionID for the project.
	 * 
	 * @param versionName
	 * @param projectId
	 * @throws IOException
	 * @return the ID for the specified Version in the specified Project
	 */
    @Override
	public String getVersionID(final String versionName, final String projectId) throws IOException {
		// Get list of versions on the specified project
		final JsonObject projectJsonObj = HttpClient
				.httpGetJSONObject(this.zapiUrl + "util/versionBoard-list?projectId=" + projectId, creds);
		if (null == projectJsonObj) {
			throw new IllegalStateException("JSONObject is null for versionName=" + versionName);
		}

		JsonArray versionOptions;

		for (String object : new String[] { "unreleasedVersions", "releasedVersions" }) {
			versionOptions = (JsonArray) projectJsonObj.get(object);

			// Iterate over versions
			for (int i = 0; i < versionOptions.size(); i++) {
				final JsonObject obj2 = versionOptions.get(i).getAsJsonObject();
				// If label matches specified version name
				if (obj2.get(ZAPI_LABEL).getAsString().equals(versionName)) {
					// Return the ID for this version
					return obj2.get(ZAPI_VALUE).getAsString();
				}
			}
		}

		throw new IllegalStateException("Version ID not found for versionName=" + versionName);
	}

	/**
	 * Gets the Cycle Details for the project keyed by Cycle IDs.
	 * 
	 * @param projectId
	 * @param versionId
	 *            - optional
	 * @param cycleId
	 *            - optional
	 * @param issueId
	 *            - optional
	 * @throws IOException
	 * @return the cycle details for the specified Version in the specified
	 *         Project
	 */
    @Override
	public Map<String, ZephyrCycle> getListOfCycles(final String projectId, final String issueId,
			final String versionId, String cycleId) throws IOException {

    	Gson gson = new Gson();
    	
		// Get list of versions on the specified project
		String url = this.zapiUrl + "cycle?projectId=" + projectId;
		if (versionId != null)
			url = url + "&versionId=" + versionId;
		if (cycleId != null)
			url = url + "&id=" + cycleId;
		if (issueId != null)
			url = url + "&issueId=" + issueId;

		final JsonObject cyclesJsonObj = HttpClient.httpGetJSONObject(url, creds);
		if (null == cyclesJsonObj) {
			throw new IllegalStateException(
					"JSONObject is null for issueId=" + issueId + " and versionId=" + versionId);
		}
		Map<String, ZephyrCycle> zephyrCycles = new HashMap<>();

		for (Map.Entry<String, JsonElement> cycleIdentifier : cyclesJsonObj.entrySet()) {
			if (!"recordsCount".equalsIgnoreCase(cycleIdentifier.getKey()))
				zephyrCycles.put(cycleIdentifier.getKey(), gson.fromJson(
						cyclesJsonObj.get(cycleIdentifier.getKey()).getAsJsonObject().toString(), ZephyrCycle.class));
		}

		return zephyrCycles;
	}
    
    @Override
	public ZephyrCycle getCycle(final String cycleId) throws IOException {
    	Gson gson = new Gson();
		// Get list of versions on the specified project
		String url = this.zapiUrl + "cycle/" + cycleId;

		final JsonObject cyclesJsonObj = HttpClient.httpGetJSONObject(url, creds);
		if (null == cyclesJsonObj) {
			throw new IllegalStateException("JSONObject is null for cycleId=" + cycleId);
		}
		return gson.fromJson(cyclesJsonObj.toString(), ZephyrCycle.class);
	}
    
    @Override
	public String createCycle(ZephyrCycle cycle) throws IOException {
    	Gson gson = new Gson();
    	
    	logger.debug("Creating Cycle because none found: {}" , cycle);
		// Get list of versions on the specified project
		String url = this.zapiUrl + "cycle";

		JsonObject response = HttpClient.post(url, creds, gson.toJson(cycle));

		if (null == response) {
			throw new IllegalStateException("JSONObject is null for create cycle=" + cycle.toString());
		}

		return response.get("responseMessage").getAsString().contains("success") ? response.get("id").getAsString()
				: null;
	}
    
    @Override
	public boolean updateCycle(ZephyrCycle cycle) throws IOException {
    	Gson gson = new Gson();
    	
		// Get list of versions on the specified project
		String url = this.zapiUrl + "cycle";
		JsonObject response = HttpClient.put(url, creds, gson.toJson(cycle));
		if (null == response) {
			throw new IllegalStateException("JSONObject is null for update cycle=" + cycle.toString());
		}
		return response.get("responseMessage").getAsString().contains("success") ? true : false;
	}

	/**
	 * Gets the Cycle Details for the project.
	 * 
	 * @param versionId
	 *            - optional
	 * @param projectId
	 * @param cycleId
	 *            - optional
	 * @param issueId
	 *            - optional
	 * @throws IOException
	 * @return the cycle details for the specified Version in the specified
	 *         Project
	 */
    @Override
	public Map<String, String> getIssueStatusIdsOfProject(final String projectId) throws IOException {
		// Get list of versions on the specified project
		String url = this.zapiUrl + "zchart/issueStatuses?projectId=" + projectId;
		Map<String, String> issueStatus = new HashMap<>();
		final JsonObject issueStatusObj = HttpClient.httpGetJSONObject(url, creds);
		if (null == issueStatusObj) {
			throw new IllegalStateException("JSONObject is null for projectId=" + projectId);
		}
		JsonArray issueStatusOptions;
		issueStatusOptions = (JsonArray) issueStatusObj.get("IssueStatusesOptionsList");

		// Iterate over versions
		for (int i = 0; i < issueStatusOptions.size(); i++) {
			final JsonObject obj2 = issueStatusOptions.get(i).getAsJsonObject();
			issueStatus.put(obj2.get(ZAPI_LABEL).getAsString(), obj2.get(ZAPI_VALUE).getAsString());
		}

		return issueStatus;
	}

	/**
	 * Updates the specified test execution
	 * 
	 * @param executionId
	 *            the ID of the execution
	 * @param status
	 *            a ZAPI.Status value
	 * @param comment
	 *            a comment for the test execution
	 * @throws IOException
	 *             put may throw IOException
	 */
    @Override
	public ZephyrExecutionDetails updateTestExecution(final String executionId, final Status status,
			final String comment) throws IOException {
    	Gson gson = new Gson();
    	
		ZephyrExecutionDetails prevExec = getTestExecutionDetails(executionId);
		DateTime currentTime = new DateTime();
		ZephyrDateParser dateParser = new ZephyrDateParser(currentTime);
		
		// Special Logic to avoid multiple updates on execution status when not needed
		if (prevExec.getExecutedOn() != null
				&& currentTime.getMillis() - dateParser.parse(prevExec.getExecutedOn()).getMillis() < MIN_TEST_SCENARIO_DELAY 
				&& (!prevExec.getExecutionStatus().equals(String.valueOf(ZephyrUpdater.Status.PASS.getValue()))
				|| prevExec.getExecutionStatus().equals(String.valueOf(status.getValue()))))
		{
			return null;
		} else {
			resetTestExecution(executionId);
			// Construct JSON object
			final JsonObject obj = new JsonObject();
			obj.addProperty("status", String.valueOf(status.getValue()));
			obj.addProperty("comment", comment);

			return gson.fromJson(
					HttpClient.put(this.zapiUrl + "execution/" + executionId + "/execute", creds, obj).toString(),
					ZephyrExecutionDetails.class);
		}
	}

	private void resetTestExecution(final String executionId) throws IOException {
		Gson gson = new Gson();
		// Construct JSON object
		final JsonObject obj = new JsonObject();
		obj.addProperty("status", String.valueOf(ZephyrUpdater.Status.UNEXECUTED.getValue()));
		obj.addProperty("comment", "RESET");

		gson.fromJson(HttpClient.put(this.zapiUrl + "execution/" + executionId + "/execute", creds, obj).toString(),
				ZephyrExecutionDetails.class);
	}
	
	@Override
	public ZephyrExecutionDetails getTestExecutionDetails(final String executionId) throws IOException {
		Gson gson = new Gson();
		final JsonObject testExecObj = HttpClient.httpGetJSONObject(this.zapiUrl + "execution/" + executionId, creds);
		return gson.fromJson(testExecObj.toString(), ZephyrExecutionDetails.class);
	}
	
	@Override
	public ZephyrExecutionDetails createTestExecution(final String issueId, final String projectId,
			final String versionId, final String cycleId, final String assigneeType, final String assignee)
			throws IOException {
		Gson gson = new Gson();
		String url = this.zapiUrl + "execution";
		// Construct JSON object
		final JsonObject obj = new JsonObject();
		obj.addProperty("cycleId", cycleId);
		obj.addProperty("issueId", issueId);
		obj.addProperty("projectId", projectId);
		obj.addProperty("versionId", versionId);

		if (assigneeType != null)
			obj.addProperty("assigneeType", assigneeType);
		if (assignee != null)
			obj.addProperty("assignee", assigneeType);

		JsonObject response = HttpClient.post(url, creds, obj);

		if (null == response) {
			throw new IllegalStateException("JSONObject is null for create execution for issueId=" + issueId);
		}
		String key = response.entrySet().iterator().next().getKey();
		return gson.fromJson(response.get(key).toString(), ZephyrExecutionDetails.class);
	}
	
	@Override
	public List<ZephyrExecutionDetails> getListOfTestExecutions(final String issueId, final String projectId,
			final String versionId, final String cycleId) throws IOException {
		Gson gson = new Gson();
		
		String url = this.zapiUrl + "execution?issueId=" + issueId;
		if (projectId != null)
			url = url + "&projectId=" + projectId;
		if (versionId != null)
			url = url + "&versionId=" + versionId;
		if (cycleId != null)
			url = url + "&cycleId=" + cycleId;

		final JsonObject executionsJsonObj = HttpClient.httpGetJSONObject(url, creds);
		if (null == executionsJsonObj) {
			throw new IllegalStateException(
					"JSONObject is null for projectId=" + projectId + " and issueId=" + issueId);
		}

		List<ZephyrExecutionDetails> zephyrExecutions = new ArrayList<>();

		JsonArray executionsOptions;
		executionsOptions = executionsJsonObj.get("executions").getAsJsonArray();

		// Iterate over versions
		for (int i = 0; i < executionsOptions.size(); i++) {
			final JsonObject obj2 = executionsOptions.get(i).getAsJsonObject();
			zephyrExecutions.add(gson.fromJson(obj2.toString(), ZephyrExecutionDetails.class));
		}

		return zephyrExecutions;
	}
	
	@Override
	public JsonObject deleteTestExecution(final List<String> executionIds) throws IOException {
		// Construct JSON object
		Gson gson = new Gson();
		
		final JsonObject obj = new JsonObject();
		JsonElement jsonElement = gson.toJsonTree(executionIds);
		obj.add("executions", jsonElement);
		return HttpClient.delete(this.zapiUrl + "execution/deleteExecutions", creds, obj.toString());
	}
}
