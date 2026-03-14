package org.notima.businessobjects.adapter.tools.command.completer;

import java.util.List;
import java.util.TreeSet;

import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Service
public class OrgNoCompleter implements Completer {

	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {

		StringsCompleter delegate = new StringsCompleter();
		TreeSet<String> orgNos = new TreeSet<String>();

		if (bofs != null) {
			for (BusinessObjectFactory bf : bofs) {
				BusinessPartnerList<Object> tenants = bf.listTenants();
				if (tenants != null && tenants.getBusinessPartner() != null) {
					for (BusinessPartner<Object> bp : tenants.getBusinessPartner()) {
						String taxId = bp.getTaxId();
						if (taxId != null && !taxId.trim().isEmpty()) {
							orgNos.add(taxId.trim());
						}
					}
				}
			}
		}

		delegate.getStrings().addAll(orgNos);
		return delegate.complete(session, commandLine, candidates);
	}

}
