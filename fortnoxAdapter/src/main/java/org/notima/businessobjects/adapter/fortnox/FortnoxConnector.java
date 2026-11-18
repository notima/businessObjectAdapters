package org.notima.businessobjects.adapter.fortnox;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

import org.notima.api.fortnox.FortnoxAuthenticationException;
import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.FortnoxConstants;
import org.notima.api.fortnox.FortnoxException;
import org.notima.api.fortnox.FortnoxScope;
import org.notima.api.fortnox.clients.FortnoxCredentials;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.FinancialYearSubset;
import org.notima.api.fortnox.entities3.FinancialYears;
import org.notima.api.fortnox.oauth2.CredentialsFile;
import org.notima.api.fortnox.oauth2.FileCredentialsProvider;
import org.notima.api.fortnox.oauth2.FortnoxOAuthLogin;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.connector.ConnectorSetting;
import org.notima.generic.businessobjects.connector.ConnectorStatus;
import org.notima.generic.ifacebusinessobjects.ObjectSyncHandler;
import org.notima.generic.ifacebusinessobjects.SieFileProvider;
import org.notima.generic.ifacebusinessobjects.SyncProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * Connects a desktop application (ie accounting-analyzer) to Fortnox through the user's own
 * integration in Fortnox Developer Portal.
 *
 * <p>Kept in the configuration directory given by the application, private to the user:</p>
 * <ul>
 *   <li>{@code integration.json}: client id, client secret, redirect port and scope;</li>
 *   <li>{@code credentials.json}: the access and refresh tokens per company (org no).</li>
 * </ul>
 * <p>Both files are created readable by the owner only (where the file system supports it).
 * The client secret is taken from the integration settings when the tokens are refreshed
 * (fortnox4j may also write it next to the refreshed tokens, in the same private file).</p>
 */
public class FortnoxConnector implements SieFileProvider, SyncProvider {

	public static final String KEY_CLIENT_ID = "clientId";
	public static final String KEY_CLIENT_SECRET = "clientSecret";
	public static final String KEY_PORT = "redirectPort";
	public static final String KEY_SCOPE = "scope";

	public static final String DEFAULT_SCOPE = FortnoxConstants.SCOPE_BOOKKEEPING + ","
			+ FortnoxConstants.SCOPE_COMPANYINFORMATION + "," + FortnoxConstants.SCOPE_SETTINGS + ","
			+ FortnoxConstants.SCOPE_CUSTOMER + "," + FortnoxConstants.SCOPE_SUPPLIER + ","
			+ FortnoxConstants.SCOPE_INVOICE + "," + FortnoxConstants.SCOPE_SUPPLIERINVOICE;

	private static final String INTEGRATION_FILE = "integration.json";
	private static final String CREDENTIALS_FILE = "credentials.json";

	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

	private File configDir;
	private Integration integration = new Integration();
	private volatile FortnoxOAuthLogin currentLogin;

	/** The integration settings, as stored in {@code integration.json}. */
	static class Integration {
		String clientId;
		String clientSecret;
		Integer redirectPort;
		String scope;
	}

	@Override
	public String getSystemName() {
		return FortnoxAdapter.SYSTEMNAME;
	}

	@Override
	public void setConfigDirectory(File dir) {
		this.configDir = dir;
		try {
			if (!dir.exists()) {
				Files.createDirectories(dir.toPath());
				CredentialsFile.restrictToOwner(dir);
			}
			loadIntegration();
		} catch (IOException e) {
			FortnoxAdapter.logger.warn("Could not read Fortnox settings in " + dir + ": " + e.getMessage());
		}
	}

	// ------------------------------------------------------------------
	// Settings
	// ------------------------------------------------------------------

	@Override
	public List<ConnectorSetting> getSettings(Locale locale) {
		boolean sv = isSwedish(locale);
		List<ConnectorSetting> list = new ArrayList<ConnectorSetting>();
		list.add(new ConnectorSetting(KEY_CLIENT_ID, "Client ID", ConnectorSetting.Type.TEXT, integration.clientId)
				.setRequired(true)
				.setDescription(sv ? "Integrationens Client ID i Fortnox Developer Portal" : "The Client ID of the integration in Fortnox Developer Portal"));
		list.add(new ConnectorSetting(KEY_CLIENT_SECRET, "Client Secret", ConnectorSetting.Type.SECRET, integration.clientSecret)
				.setRequired(true)
				.setDescription(sv ? "Integrationens Client Secret" : "The Client Secret of the integration"));
		list.add(new ConnectorSetting(KEY_PORT, sv ? "Port för redirect" : "Redirect port", ConnectorSetting.Type.NUMBER, String.valueOf(getPort()))
				.setDescription((sv ? "Registrera redirect-URI " : "Register the redirect URI ") + FortnoxOAuthLogin.redirectUri(getPort())
						+ (sv ? " på integrationen" : " on the integration")));
		List<String> scopes = new ArrayList<String>(new TreeSet<String>(FortnoxConstants.ALL_SCOPES));
		list.add(new ConnectorSetting(KEY_SCOPE, sv ? "Behörigheter (scopes)" : "Permissions (scopes)", ConnectorSetting.Type.MULTI_CHOICE, getScope().getScopesSeparatedByComma())
				.setChoices(scopes)
				.setDescription(sv ? "Måste också vara påslagna på integrationen. SIE och verifikationer kräver bookkeeping, kunder customer, leverantörer supplier, kundfakturor invoice och leverantörsfakturor supplierinvoice."
						: "Must also be enabled on the integration. SIE and vouchers need bookkeeping, customers customer, suppliers supplier, customer invoices invoice and supplier invoices supplierinvoice."));
		return list;
	}

	@Override
	public void saveSettings(Map<String, String> values) throws Exception {
		if (values.containsKey(KEY_CLIENT_ID)) integration.clientId = trimToNull(values.get(KEY_CLIENT_ID));
		if (values.containsKey(KEY_CLIENT_SECRET)) integration.clientSecret = trimToNull(values.get(KEY_CLIENT_SECRET));
		if (values.containsKey(KEY_PORT)) {
			String p = trimToNull(values.get(KEY_PORT));
			if (p==null) {
				integration.redirectPort = null;
			} else {
				int port;
				try {
					port = Integer.parseInt(p);
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException("Invalid port: " + p);
				}
				if (port<1024 || port>65535) throw new IllegalArgumentException("The port must be between 1024 and 65535: " + p);
				integration.redirectPort = port;
			}
		}
		if (values.containsKey(KEY_SCOPE)) {
			FortnoxScope scope = new FortnoxScope(values.get(KEY_SCOPE));
			if (scope.hasInvalidScopes()) throw new IllegalArgumentException("Invalid scope(s): " + String.join(", ", scope.getInvalidScopes()));
			integration.scope = scope.hasScopes() ? scope.getScopesSeparatedByComma() : null;
		}
		saveIntegration();
	}

	@Override
	public String getSetupInstructions(Locale locale) {
		String redirect = FortnoxOAuthLogin.redirectUri(getPort());
		if (isSwedish(locale)) {
			return "Skapa en egen integration i Fortnox Developer Portal (https://developer.fortnox.se):\n"
					+ "1. Ange redirect-URI " + redirect + "\n"
					+ "2. Slå på de behörigheter (scopes) du väljer här, minst Bokföring (bookkeeping).\n"
					+ "3. Fyll i integrationens Client ID och Client Secret här.\n"
					+ "Uppgifterna sparas bara för dig på den här datorn.";
		}
		return "Create your own integration in Fortnox Developer Portal (https://developer.fortnox.se):\n"
				+ "1. Set the redirect URI " + redirect + "\n"
				+ "2. Enable the permissions (scopes) chosen here, at least bookkeeping.\n"
				+ "3. Fill in the Client ID and Client Secret of the integration here.\n"
				+ "The settings are only stored for you on this computer.";
	}

	@Override
	public boolean isConfigured() {
		return integration.clientId!=null && integration.clientSecret!=null && getScope().hasScopes();
	}

	// ------------------------------------------------------------------
	// Connection
	// ------------------------------------------------------------------

	@Override
	public ConnectorStatus getStatus(String orgNo) {
		if (!isConfigured()) {
			return new ConnectorStatus(ConnectorStatus.State.NOT_CONFIGURED, "Client ID, Client Secret or scope is not set.");
		}
		FortnoxCredentials cred;
		try {
			cred = credentialsProvider(orgNo).getCredentials();
		} catch (Exception e) {
			return new ConnectorStatus(ConnectorStatus.State.ERROR, "Could not read the stored authorization: " + e.getMessage());
		}
		if (cred==null || cred.getRefreshToken()==null) {
			return new ConnectorStatus(ConnectorStatus.State.NOT_CONNECTED, "Not connected.");
		}
		if (cred.getClientId()!=null && !cred.getClientId().equals(integration.clientId)) {
			return new ConnectorStatus(ConnectorStatus.State.ERROR,
					"The company was connected with another integration (client id " + cred.getClientId() + "). Connect again.");
		}
		ConnectorStatus status = verify(orgNo);
		if (cred.getScope()!=null) {
			status.setGrantedScopes(new FortnoxScope(cred.getScope()).getScopes());
			status.setMissingScopes(FortnoxOAuthLogin.missingScopes(getScope(), cred.getScope()));
		}
		return status;
	}

	/**
	 * Calls Fortnox to check that the authorization works and belongs to the company.
	 */
	private ConnectorStatus verify(String orgNo) {
		try {
			FortnoxClient3 client = client(orgNo);
			ConnectorStatus status = new ConnectorStatus(ConnectorStatus.State.CONNECTED, "Connected.");
			try {
				CompanySetting cs = client.getCompanySetting();
				status.setRemoteName(cs.getName());
				status.setRemoteOrgNo(cs.getOrganizationNumber());
				if (cs.getOrganizationNumber()!=null && !sameOrgNo(orgNo, cs.getOrganizationNumber())) {
					status.setState(ConnectorStatus.State.ERROR);
					status.setMessage("The authorization belongs to another company: " + cs.getName() + " (" + cs.getOrganizationNumber() + ").");
				}
			} catch (FortnoxException fe) {
				// No permission to read the company settings; check bookkeeping instead
				client.getFinancialYears();
				status.setMessage("Connected (company name not available: " + fe.getMessage() + ").");
			}
			return status;
		} catch (FortnoxAuthenticationException e) {
			return new ConnectorStatus(ConnectorStatus.State.ERROR, "The authorization doesn't work, connect again: " + e.getMessage());
		} catch (Exception e) {
			return new ConnectorStatus(ConnectorStatus.State.ERROR, "Could not reach Fortnox: " + e.getMessage());
		}
	}

	@Override
	public ConnectorStatus connect(String orgNo, Consumer<URI> openBrowser) throws Exception {
		if (!isConfigured()) {
			return new ConnectorStatus(ConnectorStatus.State.NOT_CONFIGURED, "Client ID, Client Secret or scope is not set.");
		}
		FortnoxOAuthLogin login = new FortnoxOAuthLogin(integration.clientId, integration.clientSecret, getScope());
		login.setPort(getPort());
		currentLogin = login;
		FortnoxCredentials cred;
		try {
			cred = login.login(openBrowser);
		} catch (FortnoxAuthenticationException e) {
			return new ConnectorStatus(ConnectorStatus.State.NOT_CONNECTED, e.getMessage());
		} finally {
			currentLogin = null;
		}

		// Replace the earlier authorization of the company
		FileCredentialsProvider provider = credentialsProvider(orgNo);
		provider.removeAllCredentials();
		FortnoxCredentials toSave = new FortnoxCredentials();
		toSave.setOrgNo(orgNo);
		toSave.setClientId(integration.clientId);
		toSave.setAccessToken(cred.getAccessToken());
		toSave.setRefreshToken(cred.getRefreshToken());
		toSave.setExpiresIn(cred.getExpiresIn());
		toSave.setScope(cred.getScope());
		toSave.setLastRefreshToNow();
		provider.setCredentials(toSave);

		ConnectorStatus status = verify(orgNo);
		if (cred.getScope()!=null) {
			status.setGrantedScopes(new FortnoxScope(cred.getScope()).getScopes());
		}
		status.setMissingScopes(login.getMissingScopes());
		if (status.getState()==ConnectorStatus.State.ERROR && status.getRemoteOrgNo()!=null) {
			// Logged in to the wrong company: don't keep the authorization
			provider.removeAllCredentials();
		}
		return status;
	}

	@Override
	public void cancelConnect() {
		FortnoxOAuthLogin login = currentLogin;
		if (login!=null) login.cancel();
	}

	@Override
	public void disconnect(String orgNo) throws Exception {
		credentialsProvider(orgNo).removeAllCredentials();
	}

	// ------------------------------------------------------------------
	// Sync
	// ------------------------------------------------------------------

	private final List<ObjectSyncHandler<?>> syncHandlers = Collections.unmodifiableList(java.util.Arrays.<ObjectSyncHandler<?>>asList(
			new FortnoxVoucherSync(this), new FortnoxCustomerSync(this), new FortnoxSupplierSync(this),
			new FortnoxCustomerInvoiceSync(this), new FortnoxSupplierInvoiceSync(this)));

	@Override
	public List<ObjectSyncHandler<?>> getSyncHandlers() {
		return syncHandlers;
	}

	// ------------------------------------------------------------------
	// SIE
	// ------------------------------------------------------------------

	@Override
	public List<AccountingPeriod> getFinancialYears(String orgNo) throws Exception {
		FinancialYears years = client(orgNo).getFinancialYears();
		List<AccountingPeriod> result = new ArrayList<AccountingPeriod>();
		if (years!=null && years.getFinancialYearSubset()!=null) {
			for (FinancialYearSubset fy : years.getFinancialYearSubset()) {
				AccountingPeriod p = new AccountingPeriod();
				p.setPeriodStart(LocalDate.parse(fy.getFromDate()));
				p.setPeriodEnd(LocalDate.parse(fy.getToDate()));
				p.setName(fy.getFromDate() + " - " + fy.getToDate());
				result.add(p);
			}
		}
		Collections.sort(result, Comparator.comparing(AccountingPeriod::getPeriodStart).reversed());
		return result;
	}

	@Override
	public byte[] retrieveSieFile(String orgNo, LocalDate dateInYear, int sieType) throws Exception {
		FortnoxClient3 client = client(orgNo);
		FinancialYearSubset fy = client.getFinancialYear(java.sql.Date.valueOf(dateInYear!=null ? dateInYear : LocalDate.now()));
		if (fy==null) throw new Exception("No financial year in Fortnox for " + dateInYear);
		ByteBuffer buf = client.retrieveSieFile(sieType, fy.getId());
		byte[] content = new byte[buf.remaining()];
		buf.get(content);
		return content;
	}

	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------

	FortnoxClient3 client(String orgNo) throws Exception {
		return new FortnoxClient3(credentialsProvider(orgNo));
	}

	private FileCredentialsProvider credentialsProvider(String orgNo) throws IOException {
		if (configDir==null) throw new IllegalStateException("No configuration directory set");
		FileCredentialsProvider p = new FileCredentialsProvider(orgNo, new File(configDir, CREDENTIALS_FILE));
		// Used when the tokens are refreshed; the secret isn't stored with the tokens
		p.setDefaultClientId(integration.clientId);
		p.setDefaultClientSecret(integration.clientSecret);
		return p;
	}

	private FortnoxScope getScope() {
		return new FortnoxScope(integration.scope!=null ? integration.scope : DEFAULT_SCOPE);
	}

	private int getPort() {
		return integration.redirectPort!=null ? integration.redirectPort : FortnoxOAuthLogin.DEFAULT_PORT;
	}

	private void loadIntegration() throws IOException {
		File f = new File(configDir, INTEGRATION_FILE);
		if (!f.exists()) {
			integration = new Integration();
			return;
		}
		try (Reader r = new FileReader(f)) {
			Integration i = gson.fromJson(r, Integration.class);
			integration = i!=null ? i : new Integration();
		}
	}

	private void saveIntegration() throws IOException {
		if (configDir==null) throw new IllegalStateException("No configuration directory set");
		Files.createDirectories(configDir.toPath());
		File f = new File(configDir, INTEGRATION_FILE);
		if (!f.exists()) {
			Files.createFile(f.toPath());
			CredentialsFile.restrictToOwner(f);
		}
		try (Writer w = new FileWriter(f)) {
			gson.toJson(integration, w);
		}
	}

	static boolean sameOrgNo(String a, String b) {
		if (a==null || b==null) return false;
		return a.replaceAll("\\D", "").equals(b.replaceAll("\\D", ""));
	}

	private static boolean isSwedish(Locale locale) {
		return locale!=null && "sv".equals(locale.getLanguage());
	}

	private static String trimToNull(String s) {
		if (s==null) return null;
		s = s.trim();
		return s.isEmpty() ? null : s;
	}

}
