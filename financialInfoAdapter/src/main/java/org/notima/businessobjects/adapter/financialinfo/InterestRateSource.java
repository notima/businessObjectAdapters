package org.notima.businessobjects.adapter.financialinfo;

import java.time.LocalDate;
import java.util.Map;

import org.notima.generic.businessobjects.InterestRateResult;

/**
 * One source of interest rates of the {@link FinancialInfoAdapter}, ie Riksbanken.
 */
public interface InterestRateSource {

	/** @return	The keys of its rates, key to description. */
	Map<String, String> getInterestRateKeys();

	/**
	 * @return	The rate {@code rateKey} in force on {@code date}, or null if unknown.
	 * @throws Exception	If the source couldn't be read.
	 */
	InterestRateResult getInterestRate(String rateKey, LocalDate date) throws Exception;

}
