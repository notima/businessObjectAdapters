package org.notima.businessobjects.adapter.financialinfo.ecb;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.notima.businessobjects.adapter.financialinfo.ExchangeRateSource;
import org.notima.businessobjects.adapter.financialinfo.HttpFetcher;
import org.notima.generic.businessobjects.util.ExchangeRateTable;

/**
 * The euro foreign exchange reference rates of the European Central Bank: 1 EUR = x of about
 * 30 currencies, published around 16:00 CET on TARGET days, without key or limits.
 *
 * <p>The last 90 days come from {@code eurofxref-hist-90d.xml}, fetched again when it's older
 * than {@link #REFRESH}; older dates from the whole history, {@code eurofxref-hist.xml}, fetched
 * once. Both are {@code <Cube time="2026-11-11"><Cube currency="USD" rate="1.1127"/>...}.</p>
 */
public class EcbSource implements ExchangeRateSource {

	public static final String ID = "ecb";
	public static final String NAME = "European Central Bank";
	public static final String HIST_90D_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml";
	public static final String HIST_URL = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.xml";

	static final Duration REFRESH = Duration.ofHours(6);
	/** Dates this recent are looked up in the 90-day file. */
	static final int RECENT_DAYS = 85;

	private final HttpFetcher http;
	private final Clock clock;

	private NavigableMap<LocalDate, ExchangeRateTable> recent;
	private Instant recentFetched;
	private NavigableMap<LocalDate, ExchangeRateTable> history;

	public EcbSource(HttpFetcher http, Clock clock) {
		this.http = http;
		this.clock = clock;
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
	public synchronized Set<String> getCurrencies() {
		NavigableMap<LocalDate, ExchangeRateTable> known = recent != null ? recent : history;
		if (known == null || known.isEmpty()) return Collections.singleton("EUR");
		return new TreeSet<String>(known.lastEntry().getValue().getCurrencies());
	}

	@Override
	public synchronized ExchangeRateTable getTable(LocalDate date) throws IOException {
		LocalDate today = LocalDate.now(clock);
		if (!date.isBefore(today.minusDays(RECENT_DAYS))) {
			if (recent == null || recentFetched.plus(REFRESH).isBefore(clock.instant())) {
				recent = parse(http.get(HIST_90D_URL, Collections.<String, String>emptyMap()));
				recentFetched = clock.instant();
			}
			Map.Entry<LocalDate, ExchangeRateTable> e = recent.floorEntry(date);
			if (e != null) return e.getValue();
		}
		if (history == null) history = parse(http.get(HIST_URL, Collections.<String, String>emptyMap()));
		Map.Entry<LocalDate, ExchangeRateTable> e = history.floorEntry(date);
		return e != null ? e.getValue() : null;
	}

	/** The tables of an ECB rates file by date. */
	static NavigableMap<LocalDate, ExchangeRateTable> parse(String xml) throws IOException {
		NavigableMap<LocalDate, ExchangeRateTable> result = new TreeMap<LocalDate, ExchangeRateTable>();
		try {
			XMLInputFactory f = XMLInputFactory.newInstance();
			f.setProperty(XMLInputFactory.SUPPORT_DTD, false);
			f.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
			XMLStreamReader r = f.createXMLStreamReader(new StringReader(xml));
			ExchangeRateTable current = null;
			while (r.hasNext()) {
				if (r.next() != XMLStreamConstants.START_ELEMENT || !"Cube".equals(r.getLocalName())) continue;
				String time = r.getAttributeValue(null, "time");
				if (time != null) {
					current = new ExchangeRateTable("EUR", LocalDate.parse(time), NAME);
					result.put(current.getDate(), current);
					continue;
				}
				String currency = r.getAttributeValue(null, "currency");
				String rate = r.getAttributeValue(null, "rate");
				if (current != null && currency != null && rate != null) current.put(currency, new BigDecimal(rate));
			}
			r.close();
		} catch (XMLStreamException | RuntimeException e) {
			throw new IOException("Unreadable ECB rates: " + e.getMessage(), e);
		}
		return result;
	}

}
