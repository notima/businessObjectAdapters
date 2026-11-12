package org.notima.businessobjects.adapter.financialinfo;

import java.io.File;
import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeSet;

import org.notima.businessobjects.adapter.financialinfo.ecb.EcbSource;
import org.notima.businessobjects.adapter.financialinfo.riksbank.RiksbankSource;
import org.notima.generic.businessobjects.CurrencyExchangeRateResult;
import org.notima.generic.businessobjects.InterestRateResult;
import org.notima.generic.businessobjects.util.ExchangeRateTable;
import org.notima.generic.ifacebusinessobjects.ExchangeRateProvider;
import org.notima.generic.ifacebusinessobjects.InterestRateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Financial information from open sources: exchange rates of Sveriges Riksbank and the ECB,
 * interest rates of Riksbanken. More kinds (ie index numbers) are added as capability
 * interfaces of {@link org.notima.generic.ifacebusinessobjects.FinancialInfoProvider}.
 *
 * <p>Sources are asked in the order of the {@link FinancialInfoSettings settings} (Riksbanken,
 * then the ECB, by default): the first one that has both currencies on the date gives the
 * rate, so the ECB covers what Riksbanken doesn't, and steps in when Riksbanken can't be
 * reached. If no source has the rate, the result is null; if no source could be read at all,
 * the last error is thrown. Rates of past days are cached ({@link RateCache}).</p>
 *
 * <p>Registered as a {@code FinancialInfoProvider} service for {@link java.util.ServiceLoader}
 * (accounting-analyzer) and by the {@link Activator} in Karaf.</p>
 */
public class FinancialInfoAdapter implements ExchangeRateProvider, InterestRateProvider {

	public static final String SYSTEM_NAME = "FinancialInfo";

	private static final Logger log = LoggerFactory.getLogger(FinancialInfoAdapter.class);

	private final Clock clock;
	private final List<ExchangeRateSource> exchangeSources = new ArrayList<ExchangeRateSource>();
	private final List<InterestRateSource> interestSources = new ArrayList<InterestRateSource>();
	private final Map<ExchangeRateSource, RateCache> caches = new LinkedHashMap<ExchangeRateSource, RateCache>();

	/** With the settings of the settings file, or the defaults. */
	public FinancialInfoAdapter() {
		this(FinancialInfoSettings.load());
	}

	public FinancialInfoAdapter(FinancialInfoSettings settings) {
		this(settings, new JdkHttpFetcher(settings.getTimeout()), Clock.systemDefaultZone());
	}

	public FinancialInfoAdapter(FinancialInfoSettings settings, HttpFetcher http, Clock clock) {
		this.clock = clock;
		for (String id : settings.getSources()) {
			if (FinancialInfoSettings.SOURCE_RIKSBANK.equals(id)) {
				RiksbankSource rb = new RiksbankSource(http, settings.getRiksbankSubscriptionKey());
				addExchangeSource(rb, settings.getCacheDir());
				interestSources.add(rb);
			} else if (FinancialInfoSettings.SOURCE_ECB.equals(id)) {
				addExchangeSource(new EcbSource(http, clock), settings.getCacheDir());
			} else {
				log.warn("Unknown financial info source '{}' ignored", id);
			}
		}
	}

	private void addExchangeSource(ExchangeRateSource source, File cacheDir) {
		exchangeSources.add(source);
		caches.put(source, new RateCache(cacheDir != null ? new File(cacheDir, source.getId()) : null));
	}

	@Override
	public String getSystemName() {
		return SYSTEM_NAME;
	}

	@Override
	public String getDisplayName() {
		List<String> names = new ArrayList<String>();
		for (ExchangeRateSource s : exchangeSources) names.add(s.getName());
		return "Financial info (" + String.join(", ", names) + ")";
	}

	// ------------------------------------------------------------------
	// Exchange rates
	// ------------------------------------------------------------------

	@Override
	public Map<String, String> getAvailableCurrencies() {
		TreeSet<String> codes = new TreeSet<String>();
		for (ExchangeRateSource s : exchangeSources) codes.addAll(s.getCurrencies());
		Map<String, String> result = new LinkedHashMap<String, String>();
		for (String c : codes) result.put(c, currencyName(c));
		return result;
	}

	@Override
	public CurrencyExchangeRateResult getExchangeRate(String fromCurrency, String toCurrency, LocalDate date) throws Exception {
		if (fromCurrency == null || toCurrency == null || date == null) return null;
		Exception failure = null;
		boolean anyRead = false;
		for (ExchangeRateSource s : exchangeSources) {
			try {
				ExchangeRateTable t = table(s, date);
				anyRead = true;
				if (t != null && t.has(fromCurrency) && t.has(toCurrency)) return t.result(fromCurrency, toCurrency);
			} catch (Exception e) {
				log.warn("{}: no exchange rates for {}: {}", s.getName(), date, e.getMessage());
				failure = e;
			}
		}
		if (!anyRead && failure != null) throw failure;
		return null;
	}

	@Override
	public List<CurrencyExchangeRateResult> getExchangeRates(String baseCurrency, LocalDate date) throws Exception {
		if (baseCurrency == null || date == null) return Collections.emptyList();
		Exception failure = null;
		boolean anyRead = false;
		for (ExchangeRateSource s : exchangeSources) {
			try {
				ExchangeRateTable t = table(s, date);
				anyRead = true;
				if (t != null && t.has(baseCurrency)) return t.results(baseCurrency);
			} catch (Exception e) {
				log.warn("{}: no exchange rates for {}: {}", s.getName(), date, e.getMessage());
				failure = e;
			}
		}
		if (!anyRead && failure != null) throw failure;
		return Collections.emptyList();
	}

	/** The table of {@code source} for {@code date}, from the cache if it's there. */
	private ExchangeRateTable table(ExchangeRateSource source, LocalDate date) throws Exception {
		RateCache cache = caches.get(source);
		Optional<ExchangeRateTable> cached = cache.get(date, source.getName());
		if (cached != null) return cached.orElse(null);
		ExchangeRateTable t = source.getTable(date);
		cache.put(date, t, LocalDate.now(clock));
		return t;
	}

	private static String currencyName(String code) {
		try {
			return java.util.Currency.getInstance(code).getDisplayName(Locale.ENGLISH);
		} catch (IllegalArgumentException e) {
			return code;
		}
	}

	// ------------------------------------------------------------------
	// Interest rates
	// ------------------------------------------------------------------

	@Override
	public Map<String, String> getAvailableInterestRateKeys() {
		Map<String, String> keys = new LinkedHashMap<String, String>();
		for (InterestRateSource s : interestSources) {
			for (Map.Entry<String, String> e : s.getInterestRateKeys().entrySet()) keys.putIfAbsent(e.getKey(), e.getValue());
		}
		return keys;
	}

	@Override
	public InterestRateResult getInterestRate(String rateKey, LocalDate date) throws Exception {
		if (rateKey == null || date == null) return null;
		for (InterestRateSource s : interestSources) {
			if (s.getInterestRateKeys().containsKey(rateKey)) return s.getInterestRate(rateKey, date);
		}
		return null;
	}

	@Override
	public List<InterestRateResult> getInterestRates(LocalDate date) throws Exception {
		List<InterestRateResult> result = new ArrayList<InterestRateResult>();
		for (String key : getAvailableInterestRateKeys().keySet()) {
			InterestRateResult r = getInterestRate(key, date);
			if (r != null) result.add(r);
		}
		return result;
	}

}
