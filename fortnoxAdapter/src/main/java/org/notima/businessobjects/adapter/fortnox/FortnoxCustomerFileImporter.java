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
import org.notima.generic.businessobjects.Location;
import org.notima.generic.ifacebusinessobjects.BusinessPartnerFileImporter;

/**
 * Reads the customer register exported from Fortnox as CSV (kundregister.csv).
 *
 * <p>The file is UTF-8 encoded and comma-separated, with a header row of Fortnox's
 * field names ({@code name, customer_number, organisation_number, ...}). Values
 * containing commas are enclosed in double quotes.
 *
 * <p>Mapping, following {@link FortnoxConverter#convertToBusinessPartner}:
 * <ul>
 *   <li>{@code customer_number} &rarr; identity no, {@code organisation_number} &rarr; tax id,
 *       {@code vat_number} &rarr; VAT no</li>
 *   <li>{@code type} "company" &rarr; company, otherwise a private person</li>
 *   <li>{@code invoice_*} &rarr; official address (with {@code email} and {@code invoice_phone});
 *       its country code is also the partner's country code</li>
 *   <li>{@code delivery_*} &rarr; shipping address, when a delivery address is given</li>
 *   <li>{@code invoice_delivery_type} "email" &rarr; e-mail invoice</li>
 *   <li>{@code currency}, {@code payment_terms} (payment term key), {@code vat_type},
 *       {@code gln}, {@code edipeppolid} (Peppol id) and {@code www} (website)</li>
 * </ul>
 */
public class FortnoxCustomerFileImporter implements BusinessPartnerFileImporter {

	/** Header columns that identify the Fortnox customer register. */
	private static final String[] REQUIRED_COLUMNS = { "name", "customer_number", "organisation_number" };

	@Override public String   getSystemName()      { return FortnoxAdapter.SYSTEMNAME; }
	@Override public String   getFormatName()      { return "Fortnox customer register"; }
	@Override public String   getFileDescription() { return getFormatName() + " (*.csv)"; }
	@Override public String[] getFileExtensions()  { return new String[]{"csv"}; }

	/** Comma-separated, first record is the header, column names matched case-insensitively. */
	private static final CSVFormat FORMAT = CSVFormat.DEFAULT
			.withFirstRecordAsHeader()
			.withIgnoreHeaderCase()
			.withIgnoreEmptyLines();

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
				throw new IOException("Not a Fortnox customer register, expected the columns "
						+ String.join(", ", REQUIRED_COLUMNS));
			}
			for (CSVRecord record : parser) {
				Row row = new Row(record);
				if (row.get("name") == null && row.get("customer_number") == null) continue;
				partners.add(toBusinessPartner(row));
			}
		}

		BusinessPartnerFileData result = new BusinessPartnerFileData();
		result.setSourceFormat(getFormatName());
		result.setBusinessPartners(partners);
		return result;
	}

	// -------------------------------------------------------------------------
	// Mapping
	// -------------------------------------------------------------------------

	private BusinessPartner<?> toBusinessPartner(Row row) {
		BusinessPartner<?> bp = new BusinessPartner<Object>();
		bp.setName(row.get("name"));
		bp.setIdentityNo(row.get("customer_number"));
		bp.setTaxId(row.get("organisation_number"));
		bp.setVatNo(row.get("vat_number"));
		bp.setCompany("company".equalsIgnoreCase(row.get("type")));
		bp.setIsCustomer(Boolean.TRUE);
		// Missing column means active, as in Fortnox
		bp.setActive(!"0".equals(row.get("active")));
		bp.setEmailInvoice("email".equalsIgnoreCase(row.get("invoice_delivery_type")));
		bp.setCurrency(row.get("currency"));
		bp.setPaymentTermKey(row.get("payment_terms"));
		bp.setVatType(row.get("vat_type"));
		bp.setGln(row.get("gln"));
		bp.setPeppolId(row.get("edipeppolid"));
		bp.setWebsite(row.get("www"));

		Location official = new Location();
		official.setName(row.get("invoice_name"));
		official.setAddress1(row.get("invoice_address"));
		official.setAddress2(row.get("invoice_address2"));
		official.setPostal(row.get("invoice_zip_code"));
		official.setCity(row.get("invoice_city"));
		official.setCountryCode(row.get("invoice_country_code"));
		official.setEmail(row.get("email"));
		official.setPhone(row.get("invoice_phone"));
		bp.setAddressOfficial(official);
		bp.setCountryCode(official.getCountryCode());

		if (row.get("delivery_address") != null) {
			Location shipping = new Location();
			shipping.setName(row.get("delivery_name"));
			shipping.setAddress1(row.get("delivery_address"));
			shipping.setAddress2(row.get("delivery_address2"));
			shipping.setPostal(row.get("delivery_zip_code"));
			shipping.setCity(row.get("delivery_city"));
			shipping.setCountryCode(row.get("delivery_country_code"));
			shipping.setPhone(row.get("delivery_phone"));
			bp.setAddressShipping(shipping);
		}

		return bp;
	}

	/** One data row, looked up by column name. */
	private static class Row {
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
			if (reader.read() != '\uFEFF') reader.reset();
			return new CSVParser(reader, FORMAT);
		} catch (IOException | RuntimeException e) {
			reader.close();
			throw e;
		}
	}

	private static boolean hasRequiredColumns(CSVParser parser) {
		Map<String, Integer> header = parser.getHeaderMap();	// null for an empty file
		if (header == null) return false;
		Set<String> columns = new HashSet<String>();
		for (String name : header.keySet()) columns.add(name.trim().toLowerCase());
		return columns.containsAll(Arrays.asList(REQUIRED_COLUMNS));
	}

}
