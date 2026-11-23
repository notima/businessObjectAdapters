package org.notima.businessobjects.adapter.fortnox;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;

/**
 * Reads the supplier register exported from Fortnox as CSV (leverantörsregister), see
 * {@link FortnoxRegisterFileImporter} for the file format.
 *
 * <p>Mapping, following {@link FortnoxSupplierSync}:
 * <ul>
 *   <li>{@code supplier_number} &rarr; identity no, {@code organisation_number} &rarr; tax id,
 *       {@code vat_number} &rarr; VAT no; a supplier is always a company</li>
 *   <li>{@code invoice_*} &rarr; official address (with {@code email} and {@code invoice_phone});
 *       its country code is also the partner's country code</li>
 *   <li>{@code bg}, {@code pg}, {@code bank_account_number} (with {@code clearing_number} and
 *       {@code bank}), {@code iban} (with {@code bic}) &rarr; payee accounts, bankgiro preferred,
 *       and the remit-to fields, see {@link FortnoxPayeeAccounts}</li>
 *   <li>{@code active}, {@code currency}, {@code payment_terms} (payment term key),
 *       {@code vat_type}, {@code www} (website) and {@code comment}</li>
 * </ul>
 */
public class FortnoxSupplierFileImporter extends FortnoxRegisterFileImporter {

	/** Header columns that identify the Fortnox supplier register. */
	private static final String[] REQUIRED_COLUMNS = { "name", "supplier_number", "organisation_number" };

	@Override public String getFormatName() { return "Fortnox supplier register"; }
	@Override protected String[] requiredColumns() { return REQUIRED_COLUMNS; }
	@Override protected String numberColumn() { return "supplier_number"; }

	@Override
	protected BusinessPartner<?> toBusinessPartner(Row row) {
		BusinessPartner<?> bp = new BusinessPartner<Object>();
		bp.setName(row.get("name"));
		bp.setIdentityNo(row.get("supplier_number"));
		bp.setTaxId(row.get("organisation_number"));
		bp.setVatNo(row.get("vat_number"));
		bp.setCompany(true);
		bp.setIsVendor(Boolean.TRUE);
		// Missing column means active, as in Fortnox
		bp.setActive(!"0".equals(row.get("active")));
		bp.setCurrency(row.get("currency"));
		bp.setPaymentTermKey(row.get("payment_terms"));
		bp.setVatType(row.get("vat_type"));
		bp.setWebsite(row.get("www"));
		bp.setComments(row.get("comment"));

		FortnoxPayeeAccounts.Fields f = new FortnoxPayeeAccounts.Fields();
		f.bg = row.get("bg");
		f.pg = row.get("pg");
		f.bankAccountNo = row.get("bank_account_number");
		f.clearingNo = row.get("clearing_number");
		f.bank = row.get("bank");
		f.iban = row.get("iban");
		f.bic = row.get("bic");
		FortnoxPayeeAccounts.toPartner(bp, f, row.get("invoice_country_code"));

		Location official = new Location();
		official.setAddress1(row.get("invoice_address"));
		official.setAddress2(row.get("invoice_address2"));
		official.setPostal(row.get("invoice_zip_code"));
		official.setCity(row.get("invoice_city"));
		official.setCountryCode(row.get("invoice_country_code"));
		official.setEmail(row.get("email"));
		official.setPhone(row.get("invoice_phone"));
		bp.setAddressOfficial(official);
		bp.setCountryCode(official.getCountryCode());
		return bp;
	}

}
