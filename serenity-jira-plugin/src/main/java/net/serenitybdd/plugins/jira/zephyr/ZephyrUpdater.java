package net.serenitybdd.plugins.jira.zephyr;

import static ch.lambdaj.Lambda.convert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import ch.lambdaj.function.convert.Converter;
import net.serenitybdd.plugins.jira.TestResultTally;
import net.serenitybdd.plugins.jira.domain.IssueSummary;
import net.serenitybdd.plugins.jira.model.IssueTracker;
import net.serenitybdd.plugins.jira.service.JIRAConnection;
import net.serenitybdd.plugins.jira.service.SystemPropertiesJIRAConfiguration;
import net.serenitybdd.plugins.jira.zephyr.client.ZephyrClient;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrCycle;
import net.serenitybdd.plugins.jira.zephyr.domain.ZephyrExecutionDetails;
import net.thucydides.core.model.TestOutcomeSummary;
import net.thucydides.core.model.TestResult;
import net.thucydides.core.model.TestResultList;
import net.thucydides.core.util.EnvironmentVariables;

public class ZephyrUpdater {

	private static final Logger LOGGER = LoggerFactory.getLogger(ZephyrUpdater.class);
	
	private static final String SKIP_JIRA_UPDATES = "serenity.skip.jira.updates";
	private static final String JIRA_MAX_THREADS = "jira.max.threads";
	public static final String IS_ZEPHYR_ACTIVE = "serenity.jira.zephyr.active";
	public static final String ZEPHYR_DEFAULT_CYCLE = "serenity.jira.zephyr.default.cycle";
	public static final String ZEPHYR_SELECTED_CYCLE = "serenity.jira.zephyr.update.selected.cycles";
	public static final String ZEPHYR_SELECTED_VERSION = "serenity.jira.zephyr.update.selected.versions";
	
	static final int DEFAULT_MAX_THREADS = 4;
	private final JIRAConnection jiraConnection;
	private final ListeningExecutorService executorService;
	private final AtomicInteger queueSize = new AtomicInteger(0);
	private EnvironmentVariables environmentVariables;
	
	private final String projectKey;
	private final ZephyrClient zephyrClient;
	private static final String DEFAULT_CYCLE = "FUNCTIONALITY";
	private static final String DEFAULT_COMMENT= "BY SERENITY";
	
	private String jiraURL;
	
	/** Status IDs enum */
	public enum Status {
		UNEXECUTED(-1), PASS(1), FAIL(2), WIP(3), BLOCKED(4), PARTIALLY_PASS(5);

		private final int value;

		private Status(final int value) {
			this.value = value;
		}

		public int getValue() {
			return value;
		}
	}

	public ZephyrUpdater(IssueTracker issueTracker, EnvironmentVariables environmentVariables, ZephyrClient zephyrClient) {
		this.environmentVariables = environmentVariables;
		this.projectKey = environmentVariables.getProperty(SystemPropertiesJIRAConfiguration.JIRA_PROJECT);
		this.jiraConnection = issueTracker.getJiraConnection();
		this.zephyrClient = zephyrClient;
		
		executorService = MoreExecutors.listeningDecorator(Executors.newFixedThreadPool(getMaxJobs()));
		
		this.jiraURL = environmentVariables.getProperty(SystemPropertiesJIRAConfiguration.JIRA_URL);
		logStatus();
	}

	private void logStatus() {
		LOGGER.debug("ZEPHYR ACTIVE: {} ", shouldUpdateZephyr() && shouldUpdateZephyrExecution());
	}

	public void updateZephyrExecutionStatus(Set<String> issues, final TestResultTally<TestOutcomeSummary> resultTally) {
		queueSize.set(issues.size());
		for (final String issue : issues) {
			final ListenableFuture<String> future = executorService.submit(new Callable<String>() {
				@Override
				public String call() throws Exception {
					return issue;
				}
			});
			future.addListener(new Runnable() {
				@Override
				public void run() {
					logZephyrExecutionTracking(issue);
					if (!dryRun()) {
						try
						{
							updateZephyrExecution(issue, resultTally.getTestOutcomesForIssue(issue));
						} catch (IOException io)
						{
							LOGGER.error("Failed to update Zephyr Status for Issue {}", issue);
							LOGGER.error("exception occured while changing Zephyr execution status {}", io);
						}
						queueSize.decrementAndGet();
					}
				}
			}, MoreExecutors.newDirectExecutorService());
			future.addListener(new Runnable() {
				@Override
				public void run() {
					queueSize.decrementAndGet();
				}
			}, executorService);

		}
		waitTillUpdatesDone(queueSize);
	}

	private void waitTillUpdatesDone(AtomicInteger counter) {
		while (counter.get() > 0) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
			}
		}
	}
	
	private void updateZephyrExecution(String issue, List<TestOutcomeSummary> testOutcomes) throws IOException {
		IssueSummary issueDetails = this.jiraConnection.getRestJiraClient().getIssue(issue);
		LOGGER.debug("Issue Type is: {}", issueDetails.getType());
		if (("Test").equalsIgnoreCase(issueDetails.getType()) && shouldUpdateZephyrExecution() && shouldUpdateZephyr()) {
			String issueId = String.valueOf(issueDetails.getId());
			String projectId = this.jiraConnection.getRestJiraClient().getProjectByKey(projectKey).getId();
			String versionId = returnSelectedVersionId(projectId);
			Status overallResult = returnExecutionStatus(testOutcomes);
			
			if (getZephyrSelectedTestCycleNames() == null) 
			{
				updateExecutionsWithDefaultCycle(issueId, projectId, overallResult);
			} 
			else 
			{
				updateExecutionsWithSelectedCycle(issueId, projectId, versionId, overallResult);
			}
		}
	}
	
	private void updateExecutionsWithDefaultCycle(String issueId, String projectId, Status result) throws IOException 
	{
		List<ZephyrExecutionDetails> execs = this.zephyrClient.getListOfTestExecutions(issueId, projectId,null, null);
		
		if (!execs.isEmpty()) {
			for (ZephyrExecutionDetails execution : execs) {
				this.zephyrClient.updateTestExecution(execution.getId(), result, DEFAULT_COMMENT);
			}
		} else 
		{
			List<String> cycleIds = new ArrayList<>();
			Map<String, ZephyrCycle> cycles = this.zephyrClient.getListOfCycles(projectId, issueId, null, null);
			for (Map.Entry<String, ZephyrCycle> cycle : cycles.entrySet()) {
				if (cycles.get(cycle.getKey()).getName().equalsIgnoreCase(getZephyrDefaultTestCycleName())) {
					cycleIds.add(cycles.get(cycle.getKey()).getId());
				}
			}
			if (cycleIds.isEmpty()) {
				ZephyrCycle cycle = new ZephyrCycle();
				cycle.setDescription("Serenity Functional Cycle!");
				cycle.setName(getZephyrDefaultTestCycleName());
				cycle.setProjectId(projectId);
				cycle.setVersionId("-1");
				cycleIds.add(this.zephyrClient.createCycle(cycle));
			}
			
			ZephyrExecutionDetails execution;
			
			for(String cycleId: cycleIds)
			{
				execution = this.zephyrClient.createTestExecution(issueId, projectId, null, cycleId, null, null);
				this.zephyrClient.updateTestExecution(execution.getId(), result, DEFAULT_COMMENT);
			}
		}
	}
	
	private void updateExecutionsWithSelectedCycle(String issueId, String projectId, String versionId, Status result) throws IOException 
	{
		List<ZephyrExecutionDetails> execs = this.zephyrClient.getListOfTestExecutions(issueId, projectId,versionId, null);
		for (ZephyrExecutionDetails execution : execs) {
			for(String cycleName: getZephyrSelectedTestCycleNames().split(","))
			{
				if (execution.getCycleName().equalsIgnoreCase(cycleName)) {
					this.zephyrClient.updateTestExecution(execution.getId(), result, DEFAULT_COMMENT);
				}
			}
		}
	}
	
	private String returnSelectedVersionId(String projectId) throws IOException 
	{
		String versionId = null;
		if (getZephyrSelectedTestCycleVersion() != null)
			versionId = this.zephyrClient.getVersionID(getZephyrSelectedTestCycleVersion(), projectId);
		return versionId;
	}
	
	private Status returnExecutionStatus(List<TestOutcomeSummary> testOutcomes) {
		TestResult overallResult = TestResultList.overallResultFrom(convert(testOutcomes, toTestResults()));
		if (overallResult.equals(TestResult.FAILURE) || overallResult.equals(TestResult.ERROR)
				|| overallResult.equals(TestResult.COMPROMISED))
			return Status.FAIL;
		else if (overallResult.equals(TestResult.PENDING))
			return Status.WIP;
		else if (overallResult.equals(TestResult.IGNORED))
			return Status.PARTIALLY_PASS;
		else
			return Status.PASS;
	}

	private Converter<TestOutcomeSummary, TestResult> toTestResults() {
		return new Converter<TestOutcomeSummary, TestResult>() {
			@Override
			public TestResult convert(TestOutcomeSummary from) {
				return from.getTestResult();
			}
		};
	}

	private void logZephyrExecutionTracking(final String issueId) {
		if (dryRun()) {
			LOGGER.info("--- DRY RUN ONLY: ZEPHYR WILL NOT BE UPDATED ---");
		}
		LOGGER.info("Updating Zypher Execution: {}", issueId);
	}

	private boolean dryRun() {
		return Boolean.valueOf(environmentVariables.getProperty(SKIP_JIRA_UPDATES));
	}

	private int getMaxJobs() {
		return environmentVariables.getPropertyAsInteger(JIRA_MAX_THREADS, DEFAULT_MAX_THREADS);
	}
	
	private boolean shouldUpdateZephyr() {
		return !(StringUtils.isEmpty(this.jiraURL));
	}

	public boolean shouldUpdateZephyrExecution() {
		return Boolean.valueOf(environmentVariables.getPropertyAsBoolean(IS_ZEPHYR_ACTIVE, false));
	}

	protected String getZephyrSelectedTestCycleVersion() {
		return environmentVariables.getProperty(ZEPHYR_SELECTED_VERSION, null);
	}

	protected String getZephyrDefaultTestCycleName() {
		return environmentVariables.getProperty(ZEPHYR_DEFAULT_CYCLE, DEFAULT_CYCLE);
	}

	protected String getZephyrSelectedTestCycleNames() {
		return environmentVariables.getProperty(ZEPHYR_SELECTED_CYCLE, null);
	}
}