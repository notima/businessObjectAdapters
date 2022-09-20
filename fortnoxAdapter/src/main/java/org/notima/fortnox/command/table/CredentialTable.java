package org.notima.fortnox.command.table;

import java.util.Collections;
import java.util.List;

import org.notima.api.fortnox.clients.FortnoxCredentialComparatorByRefresh;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.businessobjects.adapter.tools.table.GenericTable;

public class CredentialTable extends GenericTable {

	private List<FortnoxCredentials> creds;
	
	private Long counter = 1L;
	
	public void initColumns() {
		
		column("#");
		column("ClientId");
		column("AccessToken");
		column("Type");
		column("Last refresh");
		column("Timestamp");
	
		addRows();
		
	}
	
	public CredentialTable(List<FortnoxCredentials> credentials) {
		creds = credentials;
		if (creds!=null)
			Collections.sort(creds, new FortnoxCredentialComparatorByRefresh());
		initColumns();
		
	}
	
	private void addRows() {
		
		if (creds==null || creds.size()==0) {
			this.setEmptyTableText("No credentials found");
			return;
		}
		
		for (FortnoxCredentials cred : creds) {
			
			addRow(cred);
			
		}
		
		
	}
	
	private void addRow(FortnoxCredentials cred) {
		
		if (cred.hasLegacyTokenAndClientSecret()) {
			addLegacyRow(cred);
		} else {
			addOauth2Row(cred);
		}
	
		counter++;
		
	}
	
	private void addOauth2Row(FortnoxCredentials cred) {

		this.addRow().addContent(
				counter,
				cred.getClientId(),
				cred.getAccessTokenAbbreviated(),
				(cred.hasLegacyTokenAndClientSecret() ? "Legacy" : "Oauth2"), 
				cred.getLastRefreshAsDate(),
				cred.getLastRefresh());
		
	}
	
	private void addLegacyRow(FortnoxCredentials cred) {
		
		this.addRow().addContent(
				counter,
				cred.getClientId(),
				cred.getLegacyTokenAbbreviated(),
				"Legacy", 
				"N/A",
				"N/A");
		
		
	}
	
}
