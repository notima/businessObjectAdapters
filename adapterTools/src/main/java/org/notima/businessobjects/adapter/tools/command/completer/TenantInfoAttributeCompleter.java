package org.notima.businessobjects.adapter.tools.command.completer;

import java.util.List;

import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.CommandLine;
import org.apache.karaf.shell.api.console.Completer;
import org.apache.karaf.shell.api.console.Session;
import org.apache.karaf.shell.support.completers.StringsCompleter;

/**
 * Completes settable attribute names for the set-tenant-info command.
 *
 * Covers fields on TaxSubjectIdentifier (used as the tenant key) and
 * TenantInformation itself.
 */
@Service
public class TenantInfoAttributeCompleter implements Completer {

	/** All attributes that can be set via set-tenant-info. */
	public static final String[] ATTRIBUTES = {
		"taxId",
		"countryCode",
		"legalName",
		"defaultOutputDirectory",
	};

	@Override
	public int complete(Session session, CommandLine commandLine, List<String> candidates) {
		StringsCompleter delegate = new StringsCompleter();
		for (String attr : ATTRIBUTES) {
			delegate.getStrings().add(attr);
		}
		return delegate.complete(session, commandLine, candidates);
	}

}
