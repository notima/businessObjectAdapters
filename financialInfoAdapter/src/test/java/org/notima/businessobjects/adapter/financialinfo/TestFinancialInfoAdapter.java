package org.notima.businessobjects.adapter.financialinfo;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.notima.businessobjects.adapter.financialinfo.riksbank.RiksbankSource;
import org.notima.generic.businessobjects.CurrencyExchangeRateResult;
import org.notima.generic.businessobjects.InterestRateResult;

/** Rates from saved responses of Riksbanken (2026-11-10/11) and the ECB. */
public class TestFinancialInfoAdapter {

	private static final ZoneId ZONE = ZoneId.of("Europe/Stockholm");
	private static final LocalDate TODAY = LocalDate.of(2026, 11, 12);
	private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZONE).toInstant(), ZONE);

	private StubHttp http;
	private File cacheDir;

	@Before
	public void setUp() throws IOException {
		http = new StubHttp()
				.serve("/Observations/ByGroup/130/", "riksbank-group130.json")
				.serve("/Observations/SECBREFEFF/", "riksbank-ref.json")
				.serve("eurofxref-hist-90d.xml", "ecb-90d.xml")
				.serve("eurofxref-hist.xml", "ecb-hist.xml");
		cacheDir = Files.createTempDirectory("financialinfo").toFile();
	}

	private FinancialInfoAdapter adapter(String... sources) {
		FinancialInfoSettings s = new FinancialInfoSettings().setSources(Arrays.asList(sources)).setCacheDir(cacheDir);
		return new FinancialInfoAdapter(s, http, CLOCK);
	}

	private static double rate(CurrencyExchangeRateResult r) {
		return r.getExchangeRate().getExchangeRate();
	}

	@Test
	public void riksbankRatesAgainstSek() throws Exception {
		FinancialInfoAdapter a = adapter("riksbank", "ecb");
		CurrencyExchangeRateResult eur = a.getExchangeRate("EUR", "SEK", LocalDate.of(2026, 11, 11));
		assertEquals(11.051, rate(eur), 1e-9);
		assertEquals(RiksbankSource.NAME, eur.getSource());
		assertEquals(LocalDate.of(2026, 11, 11).atStartOfDay(), eur.getExchangeRate().getValidFrom());
		// JPY is quoted per 1 yen
		assertEquals(0.06237, rate(a.getExchangeRate("JPY", "SEK", LocalDate.of(2026, 11, 11))), 1e-9);
		// Cross rate, and the inverse
		assertEquals(11.051 / 9.9317, rate(a.getExchangeRate("EUR", "USD", LocalDate.of(2026, 11, 11))), 1e-8);
		assertEquals(1 / 9.9317, rate(a.getExchangeRate("SEK", "USD", LocalDate.of(2026, 11, 11))), 1e-9);
		// A currency without a rate on the day has its latest earlier one
		assertEquals(1.03537, rate(a.getExchangeRate("NOK", "SEK", LocalDate.of(2026, 11, 11))), 1e-9);
		assertTrue(http.calls.get(0).endsWith("/Observations/ByGroup/130/2026-11-01/2026-11-11"));
	}

	@Test
	public void weekendGetsTheLastBankingDay() throws Exception {
		// Saturday: the window up to it ends with Wednesday's rates in the response
		CurrencyExchangeRateResult r = adapter("riksbank").getExchangeRate("USD", "SEK", LocalDate.of(2026, 11, 14));
		assertEquals(9.9317, rate(r), 1e-9);
		assertEquals(LocalDate.of(2026, 11, 11).atStartOfDay(), r.getExchangeRate().getValidFrom());
	}

	@Test
	public void ecbCoversWhatRiksbankDoesNotHave() throws Exception {
		FinancialInfoAdapter a = adapter("riksbank", "ecb");
		// RON isn't in the saved response of Riksbanken
		CurrencyExchangeRateResult ron = a.getExchangeRate("RON", "SEK", LocalDate.of(2026, 11, 11));
		assertEquals(11.051 / 5.3451, rate(ron), 1e-8);
		assertEquals("European Central Bank", ron.getSource());
		// An old date comes from the whole history
		CurrencyExchangeRateResult old = a.getExchangeRate("USD", "SEK", LocalDate.of(2020, 1, 4));
		assertNotNull(old);
		assertEquals(10.5045 / 1.1147, rate(old), 1e-8);
		// Unknown currency
		assertNull(a.getExchangeRate("XXX", "SEK", LocalDate.of(2026, 11, 11)));
	}

	@Test
	public void ecbStepsInWhenRiksbankIsDown() throws Exception {
		http.responses.remove("/Observations/ByGroup/130/");
		CurrencyExchangeRateResult r = adapter("riksbank", "ecb").getExchangeRate("EUR", "SEK", LocalDate.of(2026, 11, 10));
		assertEquals(10.991, rate(r), 1e-9);
		assertEquals("European Central Bank", r.getSource());
	}

	@Test(expected = IOException.class)
	public void allSourcesDownThrows() throws Exception {
		http.down = true;
		adapter("riksbank", "ecb").getExchangeRate("EUR", "SEK", LocalDate.of(2026, 11, 10));
	}

	@Test
	public void pastDaysAreCachedOnDisk() throws Exception {
		LocalDate day = LocalDate.of(2026, 11, 11);
		adapter("riksbank").getExchangeRate("EUR", "SEK", day);
		adapter("riksbank").getExchangeRate("USD", "SEK", day);   // a new adapter: from the disk
		assertEquals(1, http.callsTo("/ByGroup/130/"));
		assertTrue(new File(cacheDir, "riksbank/2026-11-11.properties").isFile());

		// Today's rates may still change: kept in memory only
		FinancialInfoAdapter a = adapter("riksbank");
		a.getExchangeRate("EUR", "SEK", TODAY);
		a.getExchangeRate("USD", "SEK", TODAY);
		assertEquals(2, http.callsTo("/ByGroup/130/"));
		assertFalse(new File(cacheDir, "riksbank/" + TODAY + ".properties").exists());
	}

	@Test
	public void subscriptionKeyIsSent() throws Exception {
		FinancialInfoSettings s = new FinancialInfoSettings().setSources(Arrays.asList("riksbank"))
				.setCacheDir(null).setRiksbankSubscriptionKey("abc");
		new FinancialInfoAdapter(s, http, CLOCK).getExchangeRate("EUR", "SEK", LocalDate.of(2026, 11, 11));
		assertEquals("abc", http.headers.get(0).get("Ocp-Apim-Subscription-Key"));
	}

	@Test
	public void referenceRate() throws Exception {
		FinancialInfoAdapter a = adapter("riksbank", "ecb");
		assertTrue(a.getAvailableInterestRateKeys().containsKey(RiksbankSource.SE_REF_RATE));
		InterestRateResult r = a.getInterestRate(RiksbankSource.SE_REF_RATE, LocalDate.of(2026, 11, 2));
		assertEquals(1.75, r.getInterestRate().getRate(), 1e-9);
		assertEquals(LocalDate.of(2026, 11, 2), r.getInterestRate().getValidFrom());
		assertEquals(1.5, a.getInterestRate(RiksbankSource.SE_REF_RATE, LocalDate.of(2026, 11, 5)).getInterestRate().getRate(), 1e-9);
		assertNull(a.getInterestRate("NO_SUCH_RATE", TODAY));
	}

	@Test
	public void referenceRateHistoryOfAYear() throws Exception {
		http.responses.clear();
		http.serve("/Observations/SECBREFEFF/", "riksbank-ref-year.json");
		FinancialInfoAdapter a = adapter("riksbank");
		java.util.List<InterestRateResult> h = a.getInterestRateHistory(RiksbankSource.SE_REF_RATE,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
		assertEquals(3, h.size());
		// In force on 1 January: the one published last before it
		assertEquals(2.0, h.get(0).getInterestRate().getRate(), 1e-9);
		assertEquals(LocalDate.of(2025, 12, 30), h.get(0).getInterestRate().getValidFrom());
		assertEquals(LocalDate.of(2026, 6, 30), h.get(0).getInterestRate().getValidUntil());
		assertEquals(1.75, h.get(1).getInterestRate().getRate(), 1e-9);
		assertEquals(LocalDate.of(2026, 7, 1), h.get(1).getInterestRate().getValidFrom());
		assertEquals(LocalDate.of(2026, 11, 2), h.get(1).getInterestRate().getValidUntil());
		assertEquals(1.5, h.get(2).getInterestRate().getRate(), 1e-9);
		assertNull(h.get(2).getInterestRate().getValidUntil());
		assertEquals(1, http.callsTo("/Observations/SECBREFEFF/2025-12-01/2026-12-31"));
		assertTrue(a.getInterestRateHistory("NO_SUCH_RATE", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31)).isEmpty());
	}

	@Test
	public void referenceRateChangesOnNewYearsDayThoughFirstPublishedLater() throws Exception {
		http.responses.clear();
		http.serve("/Observations/SECBREFEFF/", "riksbank-ref-newyear.json");
		FinancialInfoAdapter a = adapter("riksbank");
		java.util.List<InterestRateResult> h = a.getInterestRateHistory(RiksbankSource.SE_REF_RATE,
				LocalDate.of(2025, 12, 1), LocalDate.of(2026, 1, 31));
		assertEquals(2, h.size());
		assertEquals(LocalDate.of(2025, 12, 31), h.get(0).getInterestRate().getValidUntil());
		assertEquals(LocalDate.of(2026, 1, 1), h.get(1).getInterestRate().getValidFrom());
		// On 1 January, before Riksbanken has published: the rate of the new half year
		InterestRateResult r = a.getInterestRate(RiksbankSource.SE_REF_RATE, LocalDate.of(2026, 1, 1));
		assertEquals(3.0, r.getInterestRate().getRate(), 1e-9);
		assertEquals(LocalDate.of(2026, 1, 1), r.getInterestRate().getValidFrom());
		// Of the year only: the rate of the year before ended on 31 December
		java.util.List<InterestRateResult> year = a.getInterestRateHistory(RiksbankSource.SE_REF_RATE,
				LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));
		assertEquals(1, year.size());
		assertEquals(3.0, year.get(0).getInterestRate().getRate(), 1e-9);
		assertEquals(4.0, a.getInterestRate(RiksbankSource.SE_REF_RATE, LocalDate.of(2025, 12, 31)).getInterestRate().getRate(), 1e-9);
	}

	@Test
	public void availableCurrencies() throws Exception {
		FinancialInfoAdapter a = adapter("riksbank", "ecb");
		assertTrue(a.getAvailableCurrencies().containsKey("SEK"));
		assertTrue(a.getAvailableCurrencies().containsKey("JPY"));
		List<CurrencyExchangeRateResult> all = a.getExchangeRates("SEK", LocalDate.of(2026, 11, 11));
		assertEquals(4, all.size());   // EUR, JPY, NOK, USD of the saved response
	}

}
