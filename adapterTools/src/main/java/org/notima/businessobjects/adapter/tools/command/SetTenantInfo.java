package org.notima.businessobjects.adapter.tools.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.generic.businessobjects.tax.vatreport.VatReportDefinition;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.TenantInfoAttributeCompleter;
import org.notima.generic.businessobjects.SuspenseAccountType;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

@Command(scope = "notima", name = "set-tenant-info", description = "Sets an attribute on a tenant's stored TenantInformation entry. Creates the entry if it does not exist.")
@Service
public class SetTenantInfo extends AbstractAction {

	static final String SUSPENSE_ACCOUNTS_PREFIX = "suspenseAccounts.";
	static final String VAT_CODE_PREFIX = "vatCode.";

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private AdapterToolsSettings settings;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo (default from AdapterTools config)", required = false, multiValued = false)
	private String countryCode;

	@Argument(index = 0, name = "orgNo", description = "The org number of the tenant", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo;

	@Argument(index = 1, name = "attribute", description = "The attribute to set (taxId, countryCode, legalName, defaultOutputDirectory, reportDirectory, remitToAccount, remitToAccountType, remitToIBAN, remitToBIC, lastDunningDate (yyyy-MM-dd, or 'none' to clear), suspenseAccounts.UNIDENTIFIED / .IDENTIFIED / .OWNER_SETTLEMENT (accounts separated by commas, or 'none' to clear), collectionNoticeInfo.phone / .email / .remitToAccount / .remitToAccountType (BG or PG) for collection notices ('none' to clear), vatCode.<account> the account's VAT code for show-vat-report, ie vatCode.3010 MP1, 'exclude' to leave the account out of the VAT return ('none' to clear))", required = true, multiValued = false)
	@Completion(TenantInfoAttributeCompleter.class)
	private String attribute;

	@Argument(index = 2, name = "value", description = "The value to assign to the attribute", required = true, multiValued = false)
	private String value;

	@Override
	protected Object onExecute() throws Exception {

		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		if (tif == null) {
			if (settings.getTenantInformationAdapter() != null) {
				sess.getConsole().println("No TenantInformationFactory registered for configured tenantInformationAdapter " + settings.getTenantInformationAdapter() 
					+ ". Available: " + cof.listTenantInformationFactoryNames());
			} else {
				sess.getConsole().println("No TenantInformationFactory registered. Is the notima-json feature installed?");
			}
			return null;
		}

		String effectiveCountryCode = (countryCode != null && !countryCode.trim().isEmpty())
				? countryCode.trim()
				: settings.getDefaultCountryCode();

		TaxSubjectIdentifier tenantId = new TaxSubjectIdentifier(orgNo.trim(), effectiveCountryCode);

		// Load existing entry or create a new one
		TenantInformation ti = tif.getTenantInformation(tenantId);
		if (ti == null) {
			ti = new TenantInformation();
			ti.setTenant(tenantId);
		}

		// Suspense accounts of a type, ie suspenseAccounts.UNIDENTIFIED 1799,2999
		if (attribute.trim().startsWith(SUSPENSE_ACCOUNTS_PREFIX)) {
			SuspenseAccountType type = SuspenseAccountType.parse(attribute.trim().substring(SUSPENSE_ACCOUNTS_PREFIX.length()));
			if (type == null) {
				sess.getConsole().println("Unknown suspense account type in " + attribute + ". Types: " + Arrays.toString(SuspenseAccountType.values()));
				return null;
			}
			List<String> accounts = new ArrayList<String>();
			if (!"none".equalsIgnoreCase(value.trim())) {
				for (String a : value.split(",")) {
					if (!a.trim().isEmpty()) accounts.add(a.trim());
				}
			}
			ti.setSuspenseAccounts(type, accounts);
			tif.persistTenantInformation(ti);
			sess.getConsole().println("Suspense accounts " + type + " of " + tenantId + ": "
					+ (accounts.isEmpty() ? "(none)" : String.join(", ", accounts)));
			return null;
		}

		// VAT code of an account for the VAT return, ie vatCode.3010 MP1
		if (attribute.trim().startsWith(VAT_CODE_PREFIX)) {
			String account = attribute.trim().substring(VAT_CODE_PREFIX.length()).trim();
			if (account.isEmpty()) {
				sess.getConsole().println("Give the account after " + VAT_CODE_PREFIX + ", ie " + VAT_CODE_PREFIX + "3010");
				return null;
			}
			String code = noneToNull(value);
			ti.setVatCode(account, code);
			tif.persistTenantInformation(ti);
			String shown = code == null ? "(none: placed by the chart of accounts, else BAS)"
					: VatReportDefinition.EXCLUDED.equalsIgnoreCase(code) ? "EXCLUDE (not part of the VAT return)" : code.toUpperCase();
			sess.getConsole().println("VAT code of account " + account + " of " + tenantId + ": " + shown);
			return null;
		}

		// Apply the requested attribute change
		switch (attribute.trim()) {
			case "taxId":
				ti.getTenant().setTaxId(value);
				break;
			case "countryCode":
				ti.getTenant().setCountryCode(value);
				break;
			case "legalName":
				ti.getTenant().setLegalName(value);
				break;
			case "defaultOutputDirectory":
				ti.setDefaultOutputDirectory(value);
				break;
			case "reportDirectory":
				ti.setReportDirectory(value);
				break;
			case "remitToAccount":
				ti.setRemitToAccount(value);
				break;
			case "remitToAccountType":
				ti.setRemitToAccountType(value);
				break;
			case "remitToIBAN":
				ti.setRemitToIBAN(value);
				break;
			case "remitToBIC":
				ti.setRemitToBIC(value);
				break;
			case "lastDunningDate":
				ti.setLastDunningDate("none".equalsIgnoreCase(value.trim()) ? null : dfmt.parse(value.trim()));
				break;
			case "collectionNoticeInfo.phone":
				ti.getOrCreateCollectionNoticeInfo().setPhone(noneToNull(value));
				break;
			case "collectionNoticeInfo.email":
				ti.getOrCreateCollectionNoticeInfo().setEmail(noneToNull(value));
				break;
			case "collectionNoticeInfo.remitToAccount":
				ti.getOrCreateCollectionNoticeInfo().setRemitToAccount(noneToNull(value));
				break;
			case "collectionNoticeInfo.remitToAccountType":
				String type = noneToNull(value);
				if (type != null && !"BG".equalsIgnoreCase(type) && !"PG".equalsIgnoreCase(type)) {
					sess.getConsole().println("The remit to account type must be BG or PG: " + value);
					return null;
				}
				ti.getOrCreateCollectionNoticeInfo().setRemitToAccountType(type != null ? type.toUpperCase() : null);
				break;
			default:
				sess.getConsole().println("Unknown attribute: " + attribute);
				sess.getConsole().println("Available attributes: " + String.join(", ", TenantInfoAttributeCompleter.ATTRIBUTES));
				return null;
		}

		if (ti.getCollectionNoticeInfo() != null && ti.getCollectionNoticeInfo().isEmpty()) {
			ti.setCollectionNoticeInfo(null);
		}

		tif.persistTenantInformation(ti);
		sess.getConsole().println("Tenant information updated for " + tenantId + ".");

		return null;
	}

	/** Null for 'none' or a blank value. */
	private static String noneToNull(String value) {
		if (value == null) return null;
		String v = value.trim();
		return v.isEmpty() || "none".equalsIgnoreCase(v) ? null : v;
	}

}
