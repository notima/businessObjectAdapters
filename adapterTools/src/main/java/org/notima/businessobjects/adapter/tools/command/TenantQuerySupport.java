package org.notima.businessobjects.adapter.tools.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.tenant.AbstractTenantCandidateFinder;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/** What the commands that query many tenants share: choosing adapters and tenants, showing progress. */
final class TenantQuerySupport {

	private TenantQuerySupport() {}

	/**
	 * @return	The adapters named {@code systemName}, or all if null. Prints a message and
	 * 			returns an empty list if there are none.
	 */
	@SuppressWarnings("rawtypes")
	static List<BusinessObjectFactory> adapters(List<BusinessObjectFactory> bofs, String systemName, Session sess) {
		List<BusinessObjectFactory> adapters = new ArrayList<BusinessObjectFactory>();
		if (bofs != null) {
			for (BusinessObjectFactory bf : bofs) {
				if (systemName == null || systemName.equals(bf.getSystemName())) {
					adapters.add(bf);
				}
			}
		}
		if (adapters.isEmpty()) {
			sess.getConsole().println(systemName == null ? "No adapters registered" : "No adapter named " + systemName);
		}
		return adapters;
	}

	/** Limits {@code finder} to the comma separated org numbers, sets the country code and shows progress. */
	static void configure(AbstractTenantCandidateFinder<?> finder, String orgNos, String countryCode, Session sess) {
		finder.setCountryCode(countryCode);
		if (orgNos != null) {
			finder.setOnlyTaxIds(Arrays.asList(orgNos.split(",")));
		}
		finder.setProgressListener((adapterName, tenant, index, count) -> {
			// Overwritten on the same line; cleared when done
			sess.getConsole().print("\rChecking " + adapterName + " " + index + "/" + count + ": "
					+ fit(tenant.getName(), 50) + "\u001B[K");
			sess.getConsole().flush();
		});
	}

	/** Clears the progress line. */
	static void clearProgress(Session sess) {
		sess.getConsole().print("\r\u001B[K");
	}

	/** @return	The date, or today if {@code dateStr} is null. */
	static LocalDate parseDate(String dateStr) {
		return dateStr != null ? LocalDate.parse(dateStr.trim()) : LocalDate.now();
	}

	private static String fit(String s, int max) {
		if (s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

}
