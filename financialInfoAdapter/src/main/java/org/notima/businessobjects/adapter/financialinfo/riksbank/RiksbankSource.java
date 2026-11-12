package org.notima.businessobjects.adapter.financialinfo.riksbank;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.notima.businessobjects.adapter.financialinfo.ExchangeRateSource;
import org.notima.businessobjects.adapter.financialinfo.HttpFetcher;
import org.notima.businessobjects.adapter.financialinfo.InterestRateSource;
import org.notima.generic.businessobjects.InterestRate;
import org.notima.generic.businessobjects.InterestRateResult;
import org.notima.generic.businessobjects.util.ExchangeRateTable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Exchange rates and interest rates of Sveriges Riksbank, from its open SWEA API
 * ({@code https://api.riksbank.se/swea/v1}).
 *
 * <ul>
 *   <li>Exchange rates: group 130, "Currencies against Swedish kronor", series
 *       {@code SEK<currency>PMI}, in SEK per 1 unit of the currency (since 27 November 2023 for
 *       every currency). One call gets the whole group for a range of days:
 *       {@code /Observations/ByGroup/130/{from}/{to}} →
 *       {@code [{"seriesId":"SEKEURPMI","date":"2026-11-11","value":11.051}, ...]}.</li>
 *   <li>Interest rates: {@code /Observations/{seriesId}/{from}/{to}} →
 *       {@code [{"date":"2026-11-11","value":1.75}, ...]}; the reference rate
 *       ({@code SECBREFEFF}) and the policy rate ({@code SECBREPOEFF}).</li>
 * </ul>
 *
 * <p>The rate on a date is the latest published in the {@value #LOOKBACK_DAYS} days up to it,
 * which covers weekends and holidays. Without a subscription key the API allows 5 calls a
 * minute and 1000 a day per IP address; with one, sent as {@code Ocp-Apim-Subscription-Key},
 * many more. The rates are indicative; Riksbanken asks for "Source: Sveriges Riksbank".</p>
 */
public class RiksbankSource implements ExchangeRateSource, InterestRateSource {

	public static final String ID = "riksbank";
	public static final String NAME = "Sveriges Riksbank";
	public static final String BASE_URL = "https://api.riksbank.se/swea/v1";

	/** Key of the reference rate (referensräntan). */
	public static final String SE_REF_RATE = "SE_REF_RATE";
	/** Key of the policy rate (styrräntan). */
	public static final String SE_POLICY_RATE = "SE_POLICY_RATE";

	static final int LOOKBACK_DAYS = 10;
	/** Interest rates are looked further back: some are only published when they change. */
	static final int INTEREST_LOOKBACK_DAYS = 31;
	private static final String CURRENCY_GROUP = "130";

	/**
	 * The currencies of group 130 still published (those with a rate on 2026-11-11 in
	 * {@code /Observations/Latest/ByGroup/130}). Only for {@link #getCurrencies()}: the rates
	 * themselves are taken for whatever currencies a response has.
	 */
	private static final Set<String> CURRENCIES = Collections.unmodifiableSet(new LinkedHashSet<String>(Arrays.asList(
			"SEK", "AUD", "BRL", "CAD", "CHF", "CNY", "CZK", "DKK", "EUR", "GBP", "HKD", "HUF", "IDR", "ILS",
			"INR", "ISK", "JPY", "KRW", "MXN", "MYR", "NOK", "NZD", "PHP", "PLN", "RON", "SGD", "THB", "TRY",
			"USD", "ZAR")));

	private static final Map<String, String[]> RATES = new LinkedHashMap<String, String[]>();
	static {
		RATES.put(SE_REF_RATE, new String[] { "SECBREFEFF", "Referensränta (Riksbanken)" });
		RATES.put(SE_POLICY_RATE, new String[] { "SECBREPOEFF", "Styrränta (Riksbanken)" });
	}

	private final HttpFetcher http;
	private final Map<String, String> headers;
	private final String baseUrl;

	/**
	 * @param subscriptionKey	A key from developer.api.riksbank.se, or null to call without.
	 */
	public RiksbankSource(HttpFetcher http, String subscriptionKey) {
		this(http, subscriptionKey, BASE_URL);
	}

	RiksbankSource(HttpFetcher http, String subscriptionKey, String baseUrl) {
		this.http = http;
		this.baseUrl = baseUrl;
		Map<String, String> h = new HashMap<String, String>();
		h.put("Accept", "application/json");
		if (subscriptionKey != null && !subscriptionKey.trim().isEmpty()) h.put("Ocp-Apim-Subscription-Key", subscriptionKey.trim());
		this.headers = Collections.unmodifiableMap(h);
	}

	@Override
	public String getId() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public Set<String> getCurrencies() {
		return CURRENCIES;
	}

	// ------------------------------------------------------------------
	// Exchange rates
	// ------------------------------------------------------------------

	@Override
	public ExchangeRateTable getTable(LocalDate date) throws IOException {
		String url = baseUrl + "/Observations/ByGroup/" + CURRENCY_GROUP + "/" + date.minusDays(LOOKBACK_DAYS) + "/" + date;
		return parseGroup(http.get(url, headers), date);
	}

	/** The table of the latest rate of each currency on or before {@code date}, or null if none. */
	static ExchangeRateTable parseGroup(String json, LocalDate date) {
		Map<String, LocalDate> latestDate = new HashMap<String, LocalDate>();
		Map<String, BigDecimal> latestValue = new LinkedHashMap<String, BigDecimal>();
		LocalDate published = null;
		for (JsonElement e : array(json)) {
			JsonObject o = e.getAsJsonObject();
			String currency = currencyOf(string(o, "seriesId"));
			String d = string(o, "date");
			if (currency == null || d == null || !o.has("value") || o.get("value").isJsonNull()) continue;
			LocalDate day = LocalDate.parse(d);
			if (day.isAfter(date)) continue;
			LocalDate before = latestDate.get(currency);
			if (before != null && !day.isAfter(before)) continue;
			latestDate.put(currency, day);
			latestValue.put(currency, o.get("value").getAsBigDecimal());
			if (published == null || day.isAfter(published)) published = day;
		}
		if (published == null) return null;
		ExchangeRateTable t = new ExchangeRateTable("SEK", published, NAME);
		for (Map.Entry<String, BigDecimal> e : latestValue.entrySet()) t.putInverse(e.getKey(), e.getValue());
		return t;
	}

	/** "SEKEURPMI" → "EUR"; null for other series. */
	static String currencyOf(String seriesId) {
		if (seriesId == null) return null;
		String s = seriesId.trim().toUpperCase();
		return s.length() == 9 && s.startsWith("SEK") && s.endsWith("PMI") ? s.substring(3, 6) : null;
	}

	// ------------------------------------------------------------------
	// Interest rates
	// ------------------------------------------------------------------

	@Override
	public Map<String, String> getInterestRateKeys() {
		Map<String, String> keys = new LinkedHashMap<String, String>();
		for (Map.Entry<String, String[]> e : RATES.entrySet()) keys.put(e.getKey(), e.getValue()[1]);
		return keys;
	}

	@Override
	public InterestRateResult getInterestRate(String rateKey, LocalDate date) throws IOException {
		String[] rate = rateKey != null ? RATES.get(rateKey) : null;
		if (rate == null) return null;
		String url = baseUrl + "/Observations/" + rate[0] + "/" + date.minusDays(INTEREST_LOOKBACK_DAYS) + "/" + date;
		LocalDate day = null;
		BigDecimal value = null;
		for (JsonElement e : array(http.get(url, headers))) {
			JsonObject o = e.getAsJsonObject();
			String d = string(o, "date");
			if (d == null || !o.has("value") || o.get("value").isJsonNull()) continue;
			LocalDate obs = LocalDate.parse(d);
			if (obs.isAfter(date) || (day != null && !obs.isAfter(day))) continue;
			day = obs;
			value = o.get("value").getAsBigDecimal();
		}
		if (day == null) return null;
		InterestRate r = new InterestRate(rateKey, value.doubleValue()).setName(rate[1]).setValidFrom(day).setCurrency("SEK");
		return new InterestRateResult(r).setSource(NAME);
	}

	// ------------------------------------------------------------------

	private static JsonArray array(String json) {
		JsonElement root = JsonParser.parseString(json);
		return root.isJsonArray() ? root.getAsJsonArray() : new JsonArray();
	}

	private static String string(JsonObject o, String field) {
		JsonElement e = o.get(field);
		return e != null && !e.isJsonNull() ? e.getAsString() : null;
	}

}
