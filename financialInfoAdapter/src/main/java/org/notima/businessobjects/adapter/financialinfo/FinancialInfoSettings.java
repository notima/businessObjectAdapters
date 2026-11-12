package org.notima.businessobjects.adapter.financialinfo;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

/**
 * Settings of the {@link FinancialInfoAdapter}, read from a properties file: the one named by
 * the system property {@value #CONFIG_PROPERTY}, else {@code ~/.notima/financialinfo.properties}
 * if it exists. Everything has a default, so no file is needed.
 *
 * <pre>
 * # Sources in the order they're asked; the first that has a rate wins
 * sources = riksbank, ecb
 * # Optional key from developer.api.riksbank.se: more calls than 5 a minute / 1000 a day
 * riksbank.subscriptionKey =
 * # Where rates of past days are kept; "none" for no cache on disk
 * cacheDir = ~/.notima/financialinfo/cache
 * timeoutSeconds = 20
 * </pre>
 */
public class FinancialInfoSettings {

	/** System property naming the settings file. */
	public static final String CONFIG_PROPERTY = "notima.financialinfo.config";

	public static final String SOURCE_RIKSBANK = "riksbank";
	public static final String SOURCE_ECB = "ecb";

	private List<String> sources = new ArrayList<String>();
	private String riksbankSubscriptionKey;
	private File cacheDir;
	private Duration timeout = Duration.ofSeconds(20);

	public FinancialInfoSettings() {
		sources.add(SOURCE_RIKSBANK);
		sources.add(SOURCE_ECB);
		cacheDir = new File(home(), ".notima/financialinfo/cache");
	}

	/** The settings of the file, or the defaults if there's none. */
	public static FinancialInfoSettings load() {
		String path = System.getProperty(CONFIG_PROPERTY);
		File file = path != null && !path.trim().isEmpty() ? new File(path.trim())
				: new File(home(), ".notima/financialinfo.properties");
		FinancialInfoSettings s = new FinancialInfoSettings();
		if (!file.isFile()) return s;
		Properties p = new Properties();
		try (Reader r = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)) {
			p.load(r);
		} catch (IOException e) {
			return s;
		}
		return s.apply(p);
	}

	/** Applies the settings of {@code p} to these. */
	public FinancialInfoSettings apply(Properties p) {
		String src = p.getProperty("sources");
		if (src != null && !src.trim().isEmpty()) {
			sources = new ArrayList<String>();
			for (String part : src.split(",")) {
				if (!part.trim().isEmpty()) sources.add(part.trim().toLowerCase());
			}
		}
		String key = p.getProperty("riksbank.subscriptionKey");
		riksbankSubscriptionKey = key != null && !key.trim().isEmpty() ? key.trim() : null;
		String dir = p.getProperty("cacheDir");
		if (dir != null && !dir.trim().isEmpty()) {
			dir = dir.trim();
			cacheDir = "none".equalsIgnoreCase(dir) ? null
					: new File(dir.startsWith("~") ? home() + dir.substring(1) : dir);
		}
		String timeoutSeconds = p.getProperty("timeoutSeconds");
		if (timeoutSeconds != null) {
			try {
				timeout = Duration.ofSeconds(Long.parseLong(timeoutSeconds.trim()));
			} catch (NumberFormatException ignored) {
				// Keep the default
			}
		}
		return this;
	}

	private static String home() {
		return System.getProperty("user.home");
	}

	/** The sources, in the order they're asked. */
	public List<String> getSources() {
		return Collections.unmodifiableList(sources);
	}

	public FinancialInfoSettings setSources(List<String> sources) {
		this.sources = new ArrayList<String>(sources);
		return this;
	}

	public String getRiksbankSubscriptionKey() {
		return riksbankSubscriptionKey;
	}

	public FinancialInfoSettings setRiksbankSubscriptionKey(String key) {
		this.riksbankSubscriptionKey = key;
		return this;
	}

	/** Where rates of past days are kept, or null for no cache on disk. */
	public File getCacheDir() {
		return cacheDir;
	}

	public FinancialInfoSettings setCacheDir(File cacheDir) {
		this.cacheDir = cacheDir;
		return this;
	}

	public Duration getTimeout() {
		return timeout;
	}

}
