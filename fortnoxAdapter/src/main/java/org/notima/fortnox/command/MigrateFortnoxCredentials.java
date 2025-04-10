package org.notima.fortnox.command;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.api.fortnox.FortnoxCredentialsProvider;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.api.fortnox.oauth2.FortnoxOAuth2Client;

@Command(scope = "fortnox", name = "migrate-fortnox-credentials", description = "Migrate legacy Fortnox credentials to OAuth2")
@Service
public class MigrateFortnoxCredentials extends FortnoxCommand implements Action {
    @Reference
    Session session;

    @Argument(index = 0, name = "orgNo", description = "The orgno of the client", required = true, multiValued = false)
    private String orgNo = "";

    @Override
    public Object execute() throws Exception {
		bf = this.getBusinessObjectFactoryForOrgNo(orgNo);

		if (bf==null) {
			session.getConsole().println("No tenant found with orgNo [" + orgNo + "]");
			return null;
		}

		FortnoxCredentialsProvider credentialsProvider = new FileCredentialsProvider(orgNo);

        FortnoxCredentials legacyCredentials = credentialsProvider.getCredentials();
        if (legacyCredentials == null || !legacyCredentials.hasLegacyToken()) {
            session.getConsole().println("No legacy credentials found for orgNo [" + orgNo + "]");
            return null;
        }
        FortnoxOAuth2Client oAuth2Client = new FortnoxOAuth2Client();
        FortnoxCredentials credentials = oAuth2Client.migrateLegacyToken(legacyCredentials.getClientId(), legacyCredentials.getClientSecret(), legacyCredentials.getLegacyToken());
        credentialsProvider.setCredentials(credentials);

        session.getConsole().println("Migrated legacy credentials for orgNo [" + orgNo + "] to OAuth2");

        return null;
    }
}
