package org.notima.businessobjects.adapter.fortnox;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;

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
public class FortnoxCustomerFileImporter extends FortnoxRegisterFileImporter {

	/** Header columns that identify the Fortnox customer register. */
	private static final String[] REQUIRED_COLUMNS = { "name", "customer_number", "organisation_number" };

	@Override public String getFormatName() { return "Fortnox customer register"; }
	@Override protected String[] requiredColumns() { return REQUIRED_COLUMNS; }
	@Override protected String numberColumn() { return "customer_number"; }

	// -------------------------------------------------------------------------
	// Mapping
	// -------------------------------------------------------------------------

	@Override
	protected BusinessPartner<?> toBusinessPartner(Row row) {
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

}
