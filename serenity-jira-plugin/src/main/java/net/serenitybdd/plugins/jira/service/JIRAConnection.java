package net.serenitybdd.plugins.jira.service;

import net.serenitybdd.plugins.jira.guice.Injectors;

public class JIRAConnection {

    private final JIRAConfiguration configuration;

    public JIRAConnection() {
        this(Injectors.getInjector().getInstance(JIRAConfiguration.class));
    }

    public JIRAConnection(JIRAConfiguration configuration) {
        this.configuration = configuration;
    }

//    public JiraSoapService getJiraSoapService() throws MalformedURLException, RemoteException {
//        return getSoapSession().getJiraSoapService();
//    }

    protected JIRAConfiguration getConfiguration() {
        return configuration;
    }

    public String getJiraUser() {
        return getConfiguration().getJiraUser();
    }

    public String getJiraPassword() {
        return getConfiguration().getJiraPassword();
    }

    public String getJiraWebserviceUrl() {
        return getConfiguration().getJiraWebserviceUrl();
    }

    public String getProject() {
        return getConfiguration().getProject();
    }
    public void logout() { }
}
