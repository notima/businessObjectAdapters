package org.notima.businessobjects.adapter.financialinfo;

import java.time.LocalDate;
import java.util.Set;

import org.notima.generic.businessobjects.util.ExchangeRateTable;

/**
 * One source of exchange rates of the {@link FinancialInfoAdapter}, ie Riksbanken or the ECB.
 */
public interface ExchangeRateSource {

	/** @return	A short id, for the cache and the logs (ie {@code "riksbank"}). */
	String getId();

	/** @return	The name to give as the source of its rates (ie {@code "Sveriges Riksbank"}). */
	String getName();

	/** @return	The ISO 4217 codes of the currencies it has rates for, its base included. */
	Set<String> getCurrencies();

	/**
	 * @return	The rates it published last on or before {@code date}, or null if it has none
	 * 			by then.
	 * @throws Exception	If the source couldn't be read.
	 */
	ExchangeRateTable getTable(LocalDate date) throws Exception;

}
