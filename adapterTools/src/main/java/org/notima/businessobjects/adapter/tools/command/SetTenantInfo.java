package org.notima.businessobjects.adapter.tools.command;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.businessobjects.adapter.tools.command.completer.TenantInfoAttributeCompleter;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

@Command(scope = "notima", name = "set-tenant-info", description = "Sets an attribute on a tenant's stored TenantInformation entry. Creates the entry if it does not exist.")
@Service
public class SetTenantInfo extends AbstractAction {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private AdapterToolsSettings settings;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo (default from AdapterTools config)", required = false, multiValued = false)
	private String countryCode;

	@Argument(index = 0, name = "orgNo", description = "The org number of the tenant", required = true, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo;

	@Argument(index = 1, name = "attribute", description = "The attribute to set (taxId, countryCode, legalName, defaultOutputDirectory)", required = true, multiValued = false)
	@Completion(TenantInfoAttributeCompleter.class)
	private String attribute;

	@Argument(index = 2, name = "value", description = "The value to assign to the attribute", required = true, multiValued = false)
	private String value;

	@Override
	protected Object onExecute() throws Exception {

		TenantInformationFactory tif = cof.lookupFirstTenantInformationFactory();
		if (tif == null) {
			sess.getConsole().println("No TenantInformationFactory registered. Is the notima-json feature installed?");
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
			default:
				sess.getConsole().println("Unknown attribute: " + attribute);
				sess.getConsole().println("Available attributes: taxId, countryCode, legalName, defaultOutputDirectory");
				return null;
		}

		tif.persistTenantInformation(ti);
		sess.getConsole().println("Tenant information updated for " + tenantId + ".");

		return null;
	}

}
