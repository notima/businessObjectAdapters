package org.notima.businessobjects.adapter.fortnox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerFileData;
import org.notima.generic.ifacebusinessobjects.BusinessPartnerFileImporter;

/**
 * Reads a register exported from Fortnox as CSV (customers, suppliers).
 *
 * <p>The file is UTF-8 encoded and comma-separated, with a header row of Fortnox's field
 * names. Values containing commas are enclosed in double quotes. The register is recognised
 * by its {@link #requiredColumns()}.</p>
 */
public abstract class FortnoxRegisterFileImporter implements BusinessPartnerFileImporter {

	/** Comma-separated, first record is the header, column names matched case-insensitively. */
	private static final CSVFormat FORMAT = CSVFormat.DEFAULT
			.withFirstRecordAsHeader()
			.withIgnoreHeaderCase()
			.withIgnoreEmptyLines();

	@Override public String   getSystemName()      { return FortnoxAdapter.SYSTEMNAME; }
	@Override public String   getFileDescription() { return getFormatName() + " (*.csv)"; }
	@Override public String[] getFileExtensions()  { return new String[]{"csv"}; }

	/** Header columns that identify the register. */
	protected abstract String[] requiredColumns();

	/** The column with the partner's number in the register. */
	protected abstract String numberColumn();

	protected abstract BusinessPartner<?> toBusinessPartner(Row row);

	@Override
	public boolean canImport(File file) {
		try (CSVParser parser = openParser(file)) {
			return hasRequiredColumns(parser);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public BusinessPartnerFileData importFile(File file) throws IOException {
		List<BusinessPartner<?>> partners = new ArrayList<BusinessPartner<?>>();

		try (CSVParser parser = openParser(file)) {
			if (!hasRequiredColumns(parser)) {
				throw new IOException("Not a " + getFormatName() + ", expected the columns "
						+ String.join(", ", requiredColumns()));
			}
			for (CSVRecord record : parser) {
				Row row = new Row(record);
				if (row.get("name") == null && row.get(numberColumn()) == null) continue;
				partners.add(toBusinessPartner(row));
			}
		}

		BusinessPartnerFileData result = new BusinessPartnerFileData();
		result.setSourceFormat(getFormatName());
		result.setSourceSystem(FortnoxAdapter.SYSTEMNAME);
		result.setBusinessPartners(partners);
		return result;
	}

	/** One data row, looked up by column name. */
	protected static class Row {
		private final CSVRecord record;

		Row(CSVRecord record) {
			this.record = record;
		}

		/** Returns the trimmed value of {@code column}, or null if the column is missing or blank. */
		String get(String column) {
			if (!record.isMapped(column) || !record.isSet(column)) return null;
			String v = record.get(column).trim();
			return v.isEmpty() ? null : v;
		}
	}

	// -------------------------------------------------------------------------
	// CSV parsing
	// -------------------------------------------------------------------------

	private static CSVParser openParser(File file) throws IOException {
		BufferedReader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8);
		try {
			// Skip a UTF-8 byte order mark, which would otherwise end up in the first column name
			reader.mark(1);
			if (reader.read() != '﻿') reader.reset();
			return new CSVParser(reader, FORMAT);
		} catch (IOException | RuntimeException e) {
			reader.close();
			throw e;
		}
	}

	private boolean hasRequiredColumns(CSVParser parser) {
		Map<String, Integer> header = parser.getHeaderMap();	// null for an empty file
		if (header == null) return false;
		Set<String> columns = new HashSet<String>();
		for (String name : header.keySet()) columns.add(name.trim().toLowerCase());
		return columns.containsAll(Arrays.asList(requiredColumns()));
	}

}
