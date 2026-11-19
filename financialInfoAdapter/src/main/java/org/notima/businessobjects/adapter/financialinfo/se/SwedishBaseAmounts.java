package org.notima.businessobjects.adapter.financialinfo.se;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

import org.notima.generic.businessobjects.BaseAmountResult;
import org.notima.generic.ifacebusinessobjects.BaseAmountProvider;

/**
 * The Swedish base amounts, which the government sets by ordinance each autumn for the year after:
 * prisbasbelopp and förhöjt prisbasbelopp (one ordinance) and inkomstbasbelopp. There is no open
 * interface to fetch them from, so they are listed here, to be added to each year.
 *
 * <p>Sources: Skatteverket, "Kommunal fastighetsavgift kalenderåren 2008 och 2018–2026" (2025-11-11:
 * prisbasbelopp and inkomstbasbelopp with their SFS numbers); Björn Lundén, "Prisbasbelopp 2026 och
 * 2027" (2026-11-05: förhöjt prisbasbelopp, and the amounts of 2027, whose SFS numbers aren't
 * listed).</p>
 */
public final class SwedishBaseAmounts {

	public static final String SOURCE = "Regeringen (förordningar om basbelopp)";

	private static final class Amount {
		final BigDecimal amount;
		final String reference;

		Amount(long amount, String reference) {
			this.amount = BigDecimal.valueOf(amount);
			this.reference = reference;
		}
	}

	private static final Map<String, Map<Integer, Amount>> AMOUNTS = new LinkedHashMap<String, Map<Integer, Amount>>();
	private static final Map<String, String> KEYS = new LinkedHashMap<String, String>();

	static {
		Map<Integer, Amount> pbb = new TreeMap<Integer, Amount>();
		pbb.put(2008, new Amount(41000, "SFS 2007:699"));
		pbb.put(2018, new Amount(45500, "SFS 2017:865"));
		pbb.put(2019, new Amount(46500, "SFS 2018:1549"));
		pbb.put(2020, new Amount(47300, "SFS 2019:572"));
		pbb.put(2021, new Amount(47600, "SFS 2020:764"));
		pbb.put(2022, new Amount(48300, "SFS 2021:866"));
		pbb.put(2023, new Amount(52500, "SFS 2022:1398"));
		pbb.put(2024, new Amount(57300, "SFS 2023:532"));
		pbb.put(2025, new Amount(58800, "SFS 2024:663"));
		pbb.put(2026, new Amount(59200, "SFS 2025:872"));
		pbb.put(2027, new Amount(59600, null));

		// Set by the same ordinance as the prisbasbelopp
		Map<Integer, Amount> raised = new TreeMap<Integer, Amount>();
		raised.put(2018, new Amount(46500, "SFS 2017:865"));
		raised.put(2019, new Amount(47400, "SFS 2018:1549"));
		raised.put(2020, new Amount(48300, "SFS 2019:572"));
		raised.put(2021, new Amount(48600, "SFS 2020:764"));
		raised.put(2022, new Amount(49300, "SFS 2021:866"));
		raised.put(2023, new Amount(53500, "SFS 2022:1398"));
		raised.put(2024, new Amount(58500, "SFS 2023:532"));
		raised.put(2025, new Amount(60000, "SFS 2024:663"));
		raised.put(2026, new Amount(60500, "SFS 2025:872"));
		raised.put(2027, new Amount(60900, null));

		Map<Integer, Amount> ibb = new TreeMap<Integer, Amount>();
		ibb.put(2008, new Amount(48000, null));
		ibb.put(2017, new Amount(61500, "SFS 2016:940"));
		ibb.put(2018, new Amount(62500, "SFS 2017:1069"));
		ibb.put(2019, new Amount(64400, "SFS 2018:1647"));
		ibb.put(2020, new Amount(66800, "SFS 2019:692"));
		ibb.put(2021, new Amount(68200, "SFS 2020:964"));
		ibb.put(2022, new Amount(71000, "SFS 2021:992"));
		ibb.put(2023, new Amount(74300, "SFS 2022:1437"));
		ibb.put(2024, new Amount(76200, "SFS 2023:648"));
		ibb.put(2025, new Amount(80600, "SFS 2024:987"));
		ibb.put(2026, new Amount(83400, "SFS 2025:1002"));
		ibb.put(2027, new Amount(86600, null));

		AMOUNTS.put(BaseAmountProvider.SE_PRICE_BASE_AMOUNT, pbb);
		AMOUNTS.put(BaseAmountProvider.SE_RAISED_PRICE_BASE_AMOUNT, raised);
		AMOUNTS.put(BaseAmountProvider.SE_INCOME_BASE_AMOUNT, ibb);
		KEYS.put(BaseAmountProvider.SE_PRICE_BASE_AMOUNT, "Prisbasbelopp");
		KEYS.put(BaseAmountProvider.SE_RAISED_PRICE_BASE_AMOUNT, "Förhöjt prisbasbelopp");
		KEYS.put(BaseAmountProvider.SE_INCOME_BASE_AMOUNT, "Inkomstbasbelopp");
	}

	private SwedishBaseAmounts() {}

	/** @return	The keys, key to description. */
	public static Map<String, String> keys() {
		return Collections.unmodifiableMap(KEYS);
	}

	/** @return	The amount of the year, or null if it isn't listed. */
	public static BaseAmountResult get(String key, int year) {
		Map<Integer, Amount> amounts = key != null ? AMOUNTS.get(key) : null;
		Amount a = amounts != null ? amounts.get(year) : null;
		if (a == null) return null;
		return new BaseAmountResult(key, year, a.amount).setReference(a.reference).setSource(SOURCE);
	}

}
