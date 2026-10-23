package org.notima.businessobjects.adapter.tools.accounting;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriod;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.accounting.rule.AccountElementTypes;
import org.notima.generic.businessobjects.accounting.rule.CountryRuleSetRegistry;
import org.notima.generic.ifacebusinessobjects.AccountingModelProvider;

/**
 * Gives the accounting model of a tenant from the {@link AccountingModelProvider} of its
 * accounting system, through an {@link AccountingModelCache}: a cached model is used while
 * it's younger than the maximum age, else it's fetched again.
 *
 * <p>Models are classified as the accounting-analyzer classifies imported files: by the
 * {@link CountryRuleSetRegistry country rules} of the model's country, else the tenant's,
 * else the default country; accounts that still have no type get one from their category or
 * number. Cached models are classified again when read, so that changed rules apply.</p>
 */
public class AccountingModelService {

	/** By default, cached models are used for this long. */
	public static final Duration DEFAULT_MAX_AGE = Duration.ofHours(24);

	private final Supplier<Collection<AccountingModelProvider>> providers;
	private final AccountingModelCache cache;
	private final CountryRuleSetRegistry ruleSets = CountryRuleSetRegistry.withDefaults();
	private final String defaultCountryCode;
	private Duration maxAge = DEFAULT_MAX_AGE;

	/**
	 * @param providers	Gives the available providers; called at each lookup, so it can give
	 * 					the OSGi services registered at the time.
	 * @param cache		Where models are cached.
	 * @param defaultCountryCode	The country of tenants and models without one, whose rules classify their accounts.
	 */
	public AccountingModelService(Supplier<Collection<AccountingModelProvider>> providers, AccountingModelCache cache,
			String defaultCountryCode) {
		this.providers = providers;
		this.cache = cache;
		this.defaultCountryCode = defaultCountryCode;
	}

	/**
	 * @param maxAge	Cached models older than this are fetched again. {@link Duration#ZERO}
	 * 					to always fetch.
	 */
	public void setMaxAge(Duration maxAge) {
		this.maxAge = maxAge != null ? maxAge : DEFAULT_MAX_AGE;
	}

	public Duration getMaxAge() {
		return maxAge;
	}

	public AccountingModelCache getCache() {
		return cache;
	}

	/**
	 * The provider of a tenant's accounting.
	 *
	 * @param systemName	Only a provider of this system, ie the tenant's adapter; null for any.
	 * @param tenant		The tenant.
	 * @return	The first provider that has accounting for the tenant; null if none.
	 */
	public AccountingModelProvider providerFor(String systemName, TaxSubjectIdentifier tenant) {
		Collection<AccountingModelProvider> available = providers != null ? providers.get() : null;
		if (available == null) return null;
		for (AccountingModelProvider p : available) {
			if (systemName != null && !systemName.equalsIgnoreCase(p.getSystemName())) continue;
			if (p.hasAccountingFor(tenant)) return p;
		}
		return null;
	}

	/** The tenant's fiscal years, oldest first; cached as the models are. */
	public List<AccountingPeriod> fiscalYears(AccountingModelProvider provider, TaxSubjectIdentifier tenant) throws Exception {
		return fiscalYears(provider, tenant, maxAge);
	}

	/** The tenant's fiscal years, oldest first, from the cache if younger than {@code maxAge}. */
	public List<AccountingPeriod> fiscalYears(AccountingModelProvider provider, TaxSubjectIdentifier tenant, Duration maxAge) throws Exception {
		if (isFresh(cache.fiscalYearsAge(provider.getSystemName(), tenant), maxAge)) {
			List<AccountingPeriod> cached = cache.readFiscalYears(provider.getSystemName(), tenant);
			if (cached != null) return cached;
		}
		List<AccountingPeriod> years = provider.listFiscalYears(tenant);
		if (years == null) years = new ArrayList<AccountingPeriod>();
		cache.writeFiscalYears(provider.getSystemName(), tenant, years);
		return years;
	}

	/** @return	The fiscal year of the tenant that contains {@code date}; null if none does. */
	public AccountingPeriod fiscalYearOf(AccountingModelProvider provider, TaxSubjectIdentifier tenant, LocalDate date) throws Exception {
		return fiscalYearOf(provider, tenant, date, maxAge);
	}

	/** @return	The fiscal year of the tenant that contains {@code date}; null if none does. */
	public AccountingPeriod fiscalYearOf(AccountingModelProvider provider, TaxSubjectIdentifier tenant, LocalDate date, Duration maxAge) throws Exception {
		for (AccountingPeriod fy : fiscalYears(provider, tenant, maxAge)) {
			if (contains(fy, date)) return fy;
		}
		return null;
	}

	/** The model of one fiscal year, from the cache if fresh enough. */
	public AccountingFileData model(AccountingModelProvider provider, TaxSubjectIdentifier tenant, AccountingPeriod fiscalYear) throws Exception {
		return model(provider, tenant, fiscalYear, maxAge);
	}

	/** The model of one fiscal year, from the cache if younger than {@code maxAge}. */
	public AccountingFileData model(AccountingModelProvider provider, TaxSubjectIdentifier tenant, AccountingPeriod fiscalYear, Duration maxAge) throws Exception {
		String system = provider.getSystemName();
		LocalDate start = fiscalYear.getPeriodStart();
		if (isFresh(cache.modelAge(system, tenant, start), maxAge)) {
			AccountingFileData cached = cache.readModel(system, tenant, start);
			if (cached != null) {
				classify(cached, tenant);
				return cached;
			}
		}
		return refresh(provider, tenant, fiscalYear);
	}

	/** Fetches the model of one fiscal year from the provider, classifies and caches it. */
	public AccountingFileData refresh(AccountingModelProvider provider, TaxSubjectIdentifier tenant, AccountingPeriod fiscalYear) throws Exception {
		AccountingFileData data = provider.getAccountingModel(tenant, fiscalYear);
		if (data == null) return null;
		classify(data, tenant);
		cache.writeModel(provider.getSystemName(), tenant, fiscalYear.getPeriodStart(), data);
		return data;
	}

	/**
	 * The period of the fiscal year that contains {@code date}.
	 *
	 * @return	The period; null if the tenant has no fiscal year for the date.
	 */
	public AccountingPeriodData periodOf(AccountingModelProvider provider, TaxSubjectIdentifier tenant, LocalDate date) throws Exception {
		return periodOf(provider, tenant, date, maxAge);
	}

	/**
	 * The period of the fiscal year that contains {@code date}, from the cache if younger
	 * than {@code maxAge}.
	 *
	 * @return	The period; null if the tenant has no fiscal year for the date.
	 */
	public AccountingPeriodData periodOf(AccountingModelProvider provider, TaxSubjectIdentifier tenant, LocalDate date, Duration maxAge) throws Exception {
		AccountingPeriod fy = fiscalYearOf(provider, tenant, date, maxAge);
		if (fy == null) return null;
		return periodOf(model(provider, tenant, fy, maxAge), fy);
	}

	/** @return	The period of {@code data} that starts when {@code fiscalYear} does, else the one that contains its start; null if none. */
	public static AccountingPeriodData periodOf(AccountingFileData data, AccountingPeriod fiscalYear) {
		if (data == null || data.getPeriods() == null) return null;
		for (AccountingPeriodData p : data.getPeriods()) {
			if (p.getPeriodStart() != null && p.getPeriodStart().equals(fiscalYear.getPeriodStart())) return p;
		}
		for (AccountingPeriodData p : data.getPeriods()) {
			if (contains(p, fiscalYear.getPeriodStart())) return p;
		}
		return null;
	}

	private void classify(AccountingFileData data, TaxSubjectIdentifier tenant) {
		String cc = data.getCountryCode() != null ? data.getCountryCode()
				: tenant.getCountryCode() != null ? tenant.getCountryCode() : defaultCountryCode;
		ruleSets.applyTo(cc, data.getPeriods());
		AccountElementTypes.fillMissing(data.getPeriods(), ruleSets.lookup(cc));
	}

	private static boolean isFresh(Duration age, Duration maxAge) {
		return age != null && maxAge != null && !maxAge.isZero() && age.compareTo(maxAge) <= 0;
	}

	static boolean contains(AccountingPeriod p, LocalDate date) {
		if (date == null || p.getPeriodStart() == null || p.getPeriodEnd() == null) return false;
		return !date.isBefore(p.getPeriodStart()) && !date.isAfter(p.getPeriodEnd());
	}

}
