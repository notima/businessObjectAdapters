package org.notima.fortnox.command.table;

import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import org.apache.karaf.shell.support.table.ShellTable;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.businessobjects.adapter.fortnox.FileCredentialsProvider;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;

public class ClientTable extends ShellTable {

	private boolean showCredentialsInfo;

	SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	public void initColumns() {
		
		column("OrgNo");
		column("Name");

		if(showCredentialsInfo) {
			column("Credentials type");
			column("Expiry");
			column("Count").alignRight();
		}
		
	}
	
	public ClientTable(BusinessPartnerList<?> bpl, boolean showCredentialsInfo) {
		this.showCredentialsInfo = showCredentialsInfo;
		initColumns();

		for (BusinessPartner<?> bp : bpl.getBusinessPartner()) {
			addRow(bp);
		}
		
	}

	public ClientTable(BusinessPartnerList<?> bpl) {
		this(bpl, false);
	}
	
	private void addRow(BusinessPartner<?> bp) {

		if (bp==null)
			return;

		if(showCredentialsInfo) {
			String credentialsType = "";
			String credentialsExpiry = "";
			int	countCredentials = 0;
			
			try {
				FileCredentialsProvider fcp = new FileCredentialsProvider(bp.getTaxId());
				FortnoxCredentials credentials = fcp.getCredentials();
				if(credentials != null) {
					if(credentials.hasLegacyToken()) {
						credentialsType = "Legacy";
					}
					else if(credentials.hasAccessToken()) {
						credentialsType = "OAuth";
						Date expiryDate = credentials.getLastRefreshAsDate();
						Instant expiryInstant = expiryDate.toInstant();
						expiryInstant.plus(credentials.getExpiresIn(), ChronoUnit.DAYS);
						expiryDate = Date.from(expiryInstant);
						credentialsExpiry = String.format(
							"%s (%d days)",
							dateFormat.format(expiryDate),
							Duration.between(Instant.now(), expiryInstant).toDays()
						);
						countCredentials = fcp.getAllCredentials().size();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			addRow().addContent(
					bp.getTaxId(),
					bp.getName(),
					credentialsType,
					credentialsExpiry,
					countCredentials
				);
		} else {

			addRow().addContent(
				bp.getTaxId(),
				bp.getName()
			);

		}
		
	}
	
}
