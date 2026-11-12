package org.notima.businessobjects.adapter.financialinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import org.notima.generic.businessobjects.util.ExchangeRateTable;

/**
 * Exchange rate tables of one source by the date asked for: in memory, and on disk for days
 * before today, whose rates don't change any more. Saves calls (Riksbanken allows few without a
 * key) when the same day's rate is asked for again, ie when many invoices are posted.
 *
 * <p>On disk: {@code <dir>/<source>/<date asked>.properties}, holding the base currency,
 * the date the rates were published and one line per currency, as published:
 * {@code rate.<currency>} (1 base = rate) or {@code inverse.<currency>} (1 currency = rate base).</p>
 */
class RateCache {

	private final File dir;
	private final Map<LocalDate, Optional<ExchangeRateTable>> memory = new ConcurrentHashMap<LocalDate, Optional<ExchangeRateTable>>();

	/** @param dir	The directory of this source's files, or null for no cache on disk. */
	RateCache(File dir) {
		this.dir = dir;
	}

	/** @return	The cached table (possibly "none"), or null if the date isn't cached. */
	Optional<ExchangeRateTable> get(LocalDate date, String source) {
		Optional<ExchangeRateTable> t = memory.get(date);
		if (t != null) return t;
		ExchangeRateTable fromDisk = read(date, source);
		if (fromDisk != null) {
			Optional<ExchangeRateTable> o = Optional.of(fromDisk);
			memory.put(date, o);
			return o;
		}
		return null;
	}

	/**
	 * Caches {@code table} (null for "none") for {@code date}; on disk only if it's a table of
	 * a day before {@code today}.
	 */
	void put(LocalDate date, ExchangeRateTable table, LocalDate today) {
		memory.put(date, Optional.ofNullable(table));
		if (table != null && date.isBefore(today)) write(date, table);
	}

	private File file(LocalDate date) {
		return dir != null ? new File(dir, date + ".properties") : null;
	}

	private ExchangeRateTable read(LocalDate date, String source) {
		File f = file(date);
		if (f == null || !f.isFile()) return null;
		Properties p = new Properties();
		try (Reader r = new InputStreamReader(new FileInputStream(f), StandardCharsets.UTF_8)) {
			p.load(r);
			ExchangeRateTable t = new ExchangeRateTable(p.getProperty("base"), LocalDate.parse(p.getProperty("date")), source);
			for (String key : p.stringPropertyNames()) {
				if (key.startsWith("rate.")) t.put(key.substring(5), new BigDecimal(p.getProperty(key)));
				else if (key.startsWith("inverse.")) t.putInverse(key.substring(8), new BigDecimal(p.getProperty(key)));
			}
			return t;
		} catch (IOException | RuntimeException e) {
			return null;   // A broken file is fetched again
		}
	}

	private void write(LocalDate date, ExchangeRateTable t) {
		File f = file(date);
		if (f == null) return;
		Properties p = new Properties();
		p.setProperty("base", t.getBaseCurrency());
		p.setProperty("date", t.getDate().toString());
		for (String c : t.getCurrencies()) {
			if (c.equals(t.getBaseCurrency())) continue;
			p.setProperty((t.isPublishedInverse(c) ? "inverse." : "rate.") + c, t.getPublishedRate(c).toPlainString());
		}
		try {
			if (!dir.isDirectory() && !dir.mkdirs()) return;
			try (Writer w = new OutputStreamWriter(new FileOutputStream(f), StandardCharsets.UTF_8)) {
				p.store(w, "Exchange rates of " + t.getSource() + " for " + date);
			}
		} catch (IOException ignored) {
			// No cache on disk then
		}
	}

}
