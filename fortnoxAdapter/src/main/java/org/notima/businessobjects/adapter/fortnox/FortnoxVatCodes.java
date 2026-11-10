package org.notima.businessobjects.adapter.fortnox;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.notima.generic.businessobjects.accounting.rule.se.SeTaxRules;
import org.notima.generic.businessobjects.tax.BasicTaxRateProviderSE;
import org.notima.generic.businessobjects.tax.vatreport.VatReportDefinition;

/**
 * Translates the VAT codes of accounts in Fortnox to the tax keys of the canonical model: the
 * keys of the tax rules ({@link SeTaxRules}) where there is a rule, else the box of the VAT
 * return the account is reported in, or {@link VatReportDefinition#SETTLEMENT},
 * {@link VatReportDefinition#ROUNDING}.
 *
 * <p>The codes are those seen in Fortnox charts of accounts; a code that isn't known here is
 * kept as it is, so that the VAT return shows it as unknown, to be added.</p>
 */
public final class FortnoxVatCodes {

	private static final Map<String, String> KEYS;
	static {
		Map<String, String> m = new HashMap<String, String>();
		// Sales
		m.put("MP1", "MP1");
		m.put("MP2", "MP2");
		m.put("MP3", "MP3");
		m.put("MF", "MF");
		m.put("VTEU", BasicTaxRateProviderSE.KEY_EU_GOODS);
		m.put("FTEU", BasicTaxRateProviderSE.KEY_EU_SERVICES);
		m.put("OTTU", BasicTaxRateProviderSE.KEY_SE_REVERSE_CHARGE);	// Omvänd skattskyldighet, ruta 41
		// Output VAT
		m.put("U1", "10");
		m.put("U2", "11");
		m.put("U3", "12");
		m.put("UOS1", "30");
		m.put("UI25", "60");
		// Input VAT
		m.put("I", "48");
		// Purchases with reverse charge
		m.put("IVEU", "IEUV25");	// Varor från EU
		m.put("ITEU", "IEUT25");	// Tjänster från EU
		m.put("ITGLOB", "IIMT25");	// Tjänster från länder utanför EU
		m.put("IT", "IREVD");		// Tjänster i Sverige, omvänd skattskyldighet
		m.put("IV", "23");			// Varor i Sverige, omvänd skattskyldighet (no tax rule)
		// Settlement and rounding
		m.put("R1", VatReportDefinition.SETTLEMENT);	// Momsfordran
		m.put("R2", VatReportDefinition.SETTLEMENT);	// Redovisningskonto för moms
		m.put("A", VatReportDefinition.ROUNDING);		// Öres- och kronutjämning
		KEYS = Collections.unmodifiableMap(m);
	}

	private FortnoxVatCodes() {}

	/**
	 * @param vatCode	The VAT code of an account in Fortnox.
	 * @return	The tax key of the canonical model; the code itself if it isn't known; null for
	 * 			no code.
	 */
	public static String toTaxKey(String vatCode) {
		if (vatCode == null || vatCode.trim().isEmpty()) return null;
		String code = vatCode.trim().toUpperCase();
		String key = KEYS.get(code);
		return key != null ? key : code;
	}

	/** @return	True if the code is translated, see {@link #toTaxKey(String)}. */
	public static boolean isKnown(String vatCode) {
		return vatCode != null && KEYS.containsKey(vatCode.trim().toUpperCase());
	}

}
