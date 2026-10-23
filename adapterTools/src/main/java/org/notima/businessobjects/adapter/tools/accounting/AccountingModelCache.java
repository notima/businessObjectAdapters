package org.notima.businessobjects.adapter.tools.accounting;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.util.json.JsonUtil;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

/**
 * Stores the accounting models of tenants as JSON files, one per system, tenant and fiscal
 * year, so that queries over many tenants don't have to fetch them from the accounting
 * systems each time.
 *
 * <p>Files are in {@code ${karaf.data}/accounting/cache/<system>/<taxId>/}, or in the temp
 * directory when not running in Karaf: {@code <fiscal year start>.json} for a model and
 * {@code fiscal-years.json} for the list of fiscal years. When a file was written is its
 * age.</p>
 */
public class AccountingModelCache {

	public static final String CACHE_DIRECTORY_NAME = "accounting" + File.separator + "cache";
	static final String FISCAL_YEARS_FILE = "fiscal-years.json";

	private final File baseDirectory;
	private final Gson gson = JsonUtil.buildGson();

	/** A cache in the default directory. */
	public AccountingModelCache() {
		this(defaultDirectory());
	}

	public AccountingModelCache(File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	private static File defaultDirectory() {
		String karafData = System.getProperty("karaf.data");
		String base = karafData != null ? karafData : System.getProperty("java.io.tmpdir");
		return new File(base, CACHE_DIRECTORY_NAME);
	}

	public File getBaseDirectory() {
		return baseDirectory;
	}

	/** @return	The cached model of a fiscal year; null if it isn't cached. */
	public AccountingFileData readModel(String system, TaxSubjectIdentifier tenant, LocalDate fiscalYearStart) throws IOException {
		return read(modelFile(system, tenant, fiscalYearStart), AccountingFileData.class);
	}

	public void writeModel(String system, TaxSubjectIdentifier tenant, LocalDate fiscalYearStart, AccountingFileData model) throws IOException {
		write(modelFile(system, tenant, fiscalYearStart), model);
	}

	/** @return	The age of the cached model of a fiscal year; null if it isn't cached. */
	public Duration modelAge(String system, TaxSubjectIdentifier tenant, LocalDate fiscalYearStart) {
		return age(modelFile(system, tenant, fiscalYearStart));
	}

	/** @return	The cached fiscal years of a tenant; null if they aren't cached. */
	public List<AccountingPeriod> readFiscalYears(String system, TaxSubjectIdentifier tenant) throws IOException {
		Type type = new TypeToken<List<AccountingPeriod>>(){}.getType();
		return read(new File(tenantDirectory(system, tenant), FISCAL_YEARS_FILE), type);
	}

	public void writeFiscalYears(String system, TaxSubjectIdentifier tenant, List<AccountingPeriod> fiscalYears) throws IOException {
		write(new File(tenantDirectory(system, tenant), FISCAL_YEARS_FILE), fiscalYears);
	}

	/** @return	The age of the cached fiscal years of a tenant; null if they aren't cached. */
	public Duration fiscalYearsAge(String system, TaxSubjectIdentifier tenant) {
		return age(new File(tenantDirectory(system, tenant), FISCAL_YEARS_FILE));
	}

	File modelFile(String system, TaxSubjectIdentifier tenant, LocalDate fiscalYearStart) {
		return new File(tenantDirectory(system, tenant), fiscalYearStart + ".json");
	}

	File tenantDirectory(String system, TaxSubjectIdentifier tenant) {
		return new File(new File(baseDirectory, safeName(system)), safeName(tenantKey(tenant)));
	}

	/** The tenant's tax id, with its country code unless it's the only one used, ie "5560000000" or "NO_912345678". */
	static String tenantKey(TaxSubjectIdentifier tenant) {
		String taxId = tenant.getTaxId() != null ? tenant.getTaxId().replaceAll("[^A-Za-z0-9]", "").toUpperCase() : "";
		String cc = tenant.getCountryCode();
		return cc == null || cc.isEmpty() || "SE".equalsIgnoreCase(cc) ? taxId : cc.toUpperCase() + "_" + taxId;
	}

	private static String safeName(String s) {
		return s == null || s.isEmpty() ? "_" : s.replaceAll("[^A-Za-z0-9_.-]", "_");
	}

	private <T> T read(File file, Type type) throws IOException {
		if (!file.isFile()) return null;
		try (Reader r = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
			return gson.fromJson(r, type);
		}
	}

	/** Writes to a temporary file first, so that a reader never sees half a file. */
	private void write(File file, Object value) throws IOException {
		File dir = file.getParentFile();
		Files.createDirectories(dir.toPath());
		File tmp = File.createTempFile(file.getName(), ".tmp", dir);
		try {
			try (Writer w = Files.newBufferedWriter(tmp.toPath(), StandardCharsets.UTF_8)) {
				gson.toJson(value, w);
			}
			Files.move(tmp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		} finally {
			Files.deleteIfExists(tmp.toPath());
		}
	}

	private static Duration age(File file) {
		if (!file.isFile()) return null;
		return Duration.between(Instant.ofEpochMilli(file.lastModified()), Instant.now());
	}

}
