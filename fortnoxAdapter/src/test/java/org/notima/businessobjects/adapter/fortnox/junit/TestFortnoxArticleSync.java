package org.notima.businessobjects.adapter.fortnox.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.notima.api.fortnox.entities3.Article;
import org.notima.businessobjects.adapter.fortnox.FortnoxArticleSync;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductSupplier;
import org.notima.generic.businessobjects.sync.ProductSyncProfile;

/** Articles to and from Fortnox: one supplier, VAT as a rate, weight in grams. */
public class TestFortnoxArticleSync {

	private static Article article() {
		Article a = new Article();
		a.setArticleNumber("SK-1");
		a.setDescription("Träskruv 4x40");
		a.setUnit("st");
		a.setEan("7310000000017");
		a.setManufacturer("Essve");
		a.setActive(true);
		a.setType("STOCK");
		a.setWeight(250.0);
		a.setSalesAccount(3001);
		a.setPurchaseAccount(4010);
		a.setSalesPrice(2.5);
		a.setVat(25.0);
		a.setSupplierNumber("17");
		a.setSupplierName("Grossisten AB");
		a.setPurchasePrice(0.8);
		return a;
	}

	@Test
	public void fromFortnox() {
		Product<?> p = FortnoxArticleSync.toCanonical(article());
		assertEquals("SK-1", p.getKey());
		assertEquals("Träskruv 4x40", p.getName());
		assertEquals("Essve", p.getBrand());
		assertEquals(Boolean.FALSE, p.getService());
		assertEquals(0.25, p.getWeight(), 1e-9);
		assertEquals("3001", p.getSalesAccount());
		assertEquals(Double.valueOf(2.5), p.getSalesPrice());
		assertEquals(Double.valueOf(25.0), ProductSyncProfile.vatRate(p));
		ProductSupplier s = p.preferredSupplier();
		assertEquals("17", s.getSupplierKey());
		assertEquals("Grossisten AB", s.getSupplierName());
		assertEquals(Double.valueOf(0.8), s.getPurchasePrice());
	}

	@Test
	public void roundTripKeepsTheFingerprint() throws Exception {
		ProductSyncProfile profile = new ProductSyncProfile(ProductSyncProfile.Scope.SYSTEM);
		Product<?> p = FortnoxArticleSync.toCanonical(article());
		Article back = FortnoxArticleSync.toFortnox(p);
		assertEquals(Double.valueOf(250.0), back.getWeight());
		assertEquals("17", back.getSupplierNumber());
		assertEquals(Integer.valueOf(4010), back.getPurchaseAccount());
		back.setSalesPrice(2.5);   // Fortnox's, from the price list
		assertEquals(profile.fingerprint(p), profile.fingerprint(FortnoxArticleSync.toCanonical(back)));
	}

	@Test
	public void clearsWhatIsGoneAndRefusesBadAccounts() throws Exception {
		Product<Object> p = new Product<Object>();
		p.setKey("X");
		p.setName("Tjänst");
		p.setService(true);
		p.setActive(true);
		Article a = FortnoxArticleSync.toFortnox(p);
		assertEquals("SERVICE", a.getType());
		assertEquals("", a.getEan());
		assertEquals("", a.getSupplierNumber());
		assertNull(a.getSalesAccount());
		assertNull(a.getWeight());

		p.setSalesAccount("30A1");
		try {
			FortnoxArticleSync.toFortnox(p);
			assertTrue("an account must be a number", false);
		} catch (Exception expected) {
			assertTrue(expected.getMessage().contains("30A1"));
		}
		Product<?> noSupplier = FortnoxArticleSync.toCanonical(new Article() {{ setArticleNumber("Y"); setDescription("Y"); }});
		assertNull(noSupplier.getSuppliers());
		assertFalse(!noSupplier.isActive());
	}
}
