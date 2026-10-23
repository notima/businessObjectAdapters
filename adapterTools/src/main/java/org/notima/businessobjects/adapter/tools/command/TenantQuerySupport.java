package org.notima.businessobjects.adapter.tools.command;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.tenant.AbstractTenantCandidateFinder;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

/**
 * What the commands that query many tenants share: choosing adapters and tenants, showing
 * progress. The commands take the arguments {@code [adapter] [orgNo...]}, as the other
 * commands of adapterTools.
 */
final class TenantQuerySupport {

	private TenantQuerySupport() {}

	/** The adapters and tenants chosen by the arguments. */
	@SuppressWarnings("rawtypes")
	static class Selection {
		List<BusinessObjectFactory> adapters;
		/** The org numbers; null for all tenants. */
		List<String> orgNos;
	}

	/**
	 * Chooses adapters and tenants from the arguments {@code [adapter] [orgNo...]}. Org numbers
	 * can be separated by spaces or commas. An adapter argument that isn't the name of an
	 * adapter but contains digits is taken as the first org number, so that the adapter can be
	 * left out.
	 *
	 * @return	The selection; null if there's no adapter to check (a message is printed).
	 */
	@SuppressWarnings("rawtypes")
	static Selection select(List<BusinessObjectFactory> bofs, String systemName, List<String> orgNoArgs, Session sess) {
		List<String> orgNos = new ArrayList<String>();
		if (systemName != null && !isAdapter(bofs, systemName) && systemName.matches(".*\\d.*")) {
			addOrgNos(orgNos, systemName);
			systemName = null;
		}
		if (orgNoArgs != null) {
			for (String arg : orgNoArgs) addOrgNos(orgNos, arg);
		}
		Selection selection = new Selection();
		selection.adapters = adapters(bofs, systemName, sess);
		if (selection.adapters.isEmpty()) return null;
		selection.orgNos = orgNos.isEmpty() ? null : orgNos;
		return selection;
	}

	@SuppressWarnings("rawtypes")
	private static boolean isAdapter(List<BusinessObjectFactory> bofs, String systemName) {
		if (bofs == null) return false;
		for (BusinessObjectFactory bf : bofs) {
			if (systemName.equals(bf.getSystemName())) return true;
		}
		return false;
	}

	private static void addOrgNos(List<String> orgNos, String arg) {
		if (arg == null) return;
		for (String part : arg.split(",")) {
			if (!part.trim().isEmpty()) orgNos.add(part.trim());
		}
	}

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

	/**
	 * Limits {@code finder} to the org numbers, sets the country code and shows progress.
	 *
	 * @param orgNos	The org numbers; null for all tenants.
	 */
	static void configure(AbstractTenantCandidateFinder<?> finder, List<String> orgNos, String countryCode, Session sess) {
		finder.setCountryCode(countryCode);
		finder.setOnlyTaxIds(orgNos);
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
