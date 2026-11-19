package org.notima.businessobjects.adapter.financialinfo;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;
import org.notima.generic.businessobjects.BaseAmountResult;
import org.notima.generic.ifacebusinessobjects.BaseAmountProvider;

/** The Swedish base amounts, without the network. */
public class TestBaseAmounts {

	private final BaseAmountProvider adapter = new FinancialInfoAdapter(new FinancialInfoSettings());

	@Test
	public void keys() {
		assertEquals(3, adapter.getAvailableBaseAmountKeys().size());
		assertTrue(adapter.getAvailableBaseAmountKeys().containsKey(BaseAmountProvider.SE_INCOME_BASE_AMOUNT));
	}

	@Test
	public void amountsOf2026() throws Exception {
		BaseAmountResult pbb = adapter.getBaseAmount(BaseAmountProvider.SE_PRICE_BASE_AMOUNT, 2026);
		assertEquals(0, new BigDecimal(59200).compareTo(pbb.getAmount()));
		assertEquals("SFS 2025:872", pbb.getReference());
		assertEquals(2026, pbb.getYear());
		assertNotNull(pbb.getSource());
		assertEquals(0, new BigDecimal(60500).compareTo(
				adapter.getBaseAmount(BaseAmountProvider.SE_RAISED_PRICE_BASE_AMOUNT, 2026).getAmount()));
		assertEquals(0, new BigDecimal(83400).compareTo(
				adapter.getBaseAmount(BaseAmountProvider.SE_INCOME_BASE_AMOUNT, 2026).getAmount()));
	}

	@Test
	public void unknownYearOrKey() throws Exception {
		assertNull(adapter.getBaseAmount(BaseAmountProvider.SE_INCOME_BASE_AMOUNT, 1990));
		assertNull(adapter.getBaseAmount("XX", 2026));
		assertNull(adapter.getBaseAmount(null, 2026));
	}

}
