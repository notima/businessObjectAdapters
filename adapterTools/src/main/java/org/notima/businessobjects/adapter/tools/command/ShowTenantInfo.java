package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Completion;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.AdapterToolsSettings;
import org.notima.businessobjects.adapter.tools.CanonicalObjectFactory;
import org.notima.businessobjects.adapter.tools.command.completer.OrgNoCompleter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

@Command(scope = "notima", name = "show-tenant-info", description = "Shows stored tenant information. Falls back to listing tenants from registered adapters if no stored info is found.")
@Service
public class ShowTenantInfo extends AbstractAction {

	@Reference
	private CanonicalObjectFactory cof;

	@Reference
	private AdapterToolsSettings settings;

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Argument(index = 0, name = "orgNo", description = "The org number of the tenant to look up", required = false, multiValued = false)
	@Completion(OrgNoCompleter.class)
	private String orgNo;

	@Option(name = "-co", aliases = { "--country-code" }, description = "Country code for the orgNo (default from AdapterTools config)", required = false, multiValued = false)
	private String countryCode;

	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	protected Object onExecute() throws Exception {

		String effectiveCountryCode = (countryCode != null && !countryCode.trim().isEmpty())
				? countryCode.trim()
				: settings.getDefaultCountryCode();

		TenantInformationFactory tif = cof.lookupFirstTenantInformationFactory();

		if (orgNo != null && !orgNo.trim().isEmpty()) {
			TaxSubjectIdentifier tenantId = new TaxSubjectIdentifier(orgNo.trim(), effectiveCountryCode);
			TenantInformation ti = (tif != null) ? tif.getTenantInformation(tenantId) : null;

			if (ti != null) {
				printTenantInformation(ti);
			} else {
				sess.getConsole().println("No stored tenant information found for " + tenantId + ".");
				sess.getConsole().println("Querying registered adapters...");
				boolean found = false;
				if (bofs != null) {
					for (BusinessObjectFactory bf : bofs) {
						BusinessPartnerList<Object> bpl = bf.listTenants();
						if (bpl != null && bpl.getBusinessPartner() != null) {
							for (BusinessPartner<Object> bp : bpl.getBusinessPartner()) {
								if (orgNo.trim().equalsIgnoreCase(bp.getTaxId())
										|| orgNo.trim().equalsIgnoreCase(bp.getIdentityNo())) {
									sess.getConsole().println("Found in adapter: " + bf.getSystemName());
									printBusinessPartner(bp);
									found = true;
								}
							}
						}
					}
				}
				if (!found) {
					sess.getConsole().println("Tenant " + orgNo + " not found in any registered adapter.");
				}
			}
		} else {
			sess.getConsole().println("Specify an orgNo to look up tenant information.");
			sess.getConsole().println("Example: notima:show-tenant-info 559144-8740");
		}

		return null;
	}

	private void printTenantInformation(TenantInformation ti) {
		sess.getConsole().println("Tenant Information");
		sess.getConsole().println("------------------");
		if (ti.getTenant() != null) {
			TaxSubjectIdentifier t = ti.getTenant();
			if (t.getTaxId() != null)
				sess.getConsole().println("  Org No / Tax ID  : " + t.getTaxId());
			if (t.getCountryCode() != null)
				sess.getConsole().println("  Country Code     : " + t.getCountryCode());
			if (t.getLegalName() != null)
				sess.getConsole().println("  Legal Name       : " + t.getLegalName());
		}
		String outDir = ti.getDefaultOutputDirectory();
		sess.getConsole().println("  Default Output Dir: " + (outDir != null ? outDir : "(not set)"));
	}

	private void printBusinessPartner(BusinessPartner<?> bp) {
		sess.getConsole().println("  Name        : " + bp.getName());
		sess.getConsole().println("  Tax ID      : " + bp.getTaxId());
		sess.getConsole().println("  Identity No : " + bp.getIdentityNo());
	}

}
