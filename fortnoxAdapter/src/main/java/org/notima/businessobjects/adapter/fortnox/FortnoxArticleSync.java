package org.notima.businessobjects.adapter.fortnox;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.Article;
import org.notima.api.fortnox.entities3.ArticleSubset;
import org.notima.api.fortnox.entities3.Articles;
import org.notima.api.fortnox.entities3.Price;
import org.notima.api.fortnox.entities3.PriceListSubset;
import org.notima.api.fortnox.entities3.PriceLists;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductSupplier;
import org.notima.generic.businessobjects.sync.ProductSyncProfile;
import org.notima.generic.businessobjects.sync.RemoteObject;
import org.notima.generic.businessobjects.sync.SyncLink;
import org.notima.generic.businessobjects.sync.SyncOperation;
import org.notima.generic.businessobjects.sync.SyncProfile;
import org.notima.generic.businessobjects.sync.SyncQuery;
import org.notima.generic.ifacebusinessobjects.ObjectSyncHandler;

/**
 * Syncs articles with Fortnox's article register. The remote key is the article number, the same
 * here and there.
 *
 * <ul>
 *   <li>Compared and written: what Fortnox has ({@link ProductSyncProfile.Scope#SYSTEM}): name,
 *       unit, EAN, manufacturer (brand), weight (g here kg) and measures (mm), goods or service,
 *       active, VAT rate, sales and purchase account, sales price, and one supplier (Fortnox's
 *       supplier number, in {@link ProductSupplier#getSupplierKey()}) with the purchase price.</li>
 *   <li>The VAT is a rate in {@link ProductSyncProfile#VAT_RATE_ATTRIBUTE}, and the supplier is
 *       Fortnox's supplier number; the caller's translator turns them into the company's VAT key
 *       and supplier.</li>
 *   <li>The sales price is the price of Fortnox's preselected price list (normally A) from
 *       quantity 0: Fortnox keeps an article's sales price in its price lists, so it's written
 *       there.</li>
 *   <li>What only Fortnox has (stock, housework, webshop ...) is left as it is.</li>
 * </ul>
 */
public class FortnoxArticleSync implements ObjectSyncHandler<Product<?>> {

	public static final String REGISTER = "article";

	/** Fortnox's clock and ours may differ a little. */
	private static final int MODIFIED_MARGIN_MINUTES = 10;

	private static final String TYPE_SERVICE = "SERVICE";
	private static final String TYPE_STOCK = "STOCK";

	private final FortnoxConnector connector;
	private final ProductSyncProfile profile = new ProductSyncProfile(ProductSyncProfile.Scope.SYSTEM);
	/** The preselected price list per company. */
	private final Map<String, String> defaultPriceLists = new ConcurrentHashMap<String, String>();

	public FortnoxArticleSync(FortnoxConnector connector) {
		this.connector = connector;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	public Class<Product<?>> getType() {
		return (Class) Product.class;
	}

	@Override
	public String getRegister() {
		return REGISTER;
	}

	@Override
	public Set<SyncOperation> getSupportedOperations() {
		return Collections.unmodifiableSet(EnumSet.of(SyncOperation.READ, SyncOperation.CREATE, SyncOperation.UPDATE));
	}

	@Override
	public SyncProfile<Product<?>> getProfile() {
		return profile;
	}

	/**
	 * All articles' summaries, and when synced before, the linked ones changed since (Fortnox's
	 * {@code lastmodified}); the linked unchanged ones are left out, compared by fingerprints.
	 */
	@Override
	public List<RemoteObject<Product<?>>> list(String orgNo, SyncQuery query) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		Set<String> changed = new HashSet<String>();
		if (query.getModifiedSince() != null && !query.getLinkedRemoteKeys().isEmpty()) {
			LocalDateTime since = query.getModifiedSince().minusMinutes(MODIFIED_MARGIN_MINUTES);
			Articles recent = client.getArticles(FortnoxClient3.lastModifiedFilter(since));
			if (recent != null && recent.getArticleSubset() != null) {
				for (ArticleSubset a : recent.getArticleSubset()) changed.add(a.getArticleNumber());
			}
		}
		List<RemoteObject<Product<?>>> result = new ArrayList<RemoteObject<Product<?>>>();
		Articles all = client.getArticles((String) null);
		if (all == null || all.getArticleSubset() == null) return result;
		for (ArticleSubset a : all.getArticleSubset()) {
			String key = empty(a.getArticleNumber());
			if (key == null) continue;
			if (query.getLinkedRemoteKeys().contains(key) && !changed.contains(key)) continue;
			Product<Object> p = new Product<Object>();
			p.setKey(key);
			p.setName(a.getDescription());
			p.setUnit(empty(a.getUnit()));
			p.setUpc(empty(a.getEan()));
			p.setSalesPrice(a.getSalesPrice());
			result.add(new RemoteObject<Product<?>>(key, p, false));
		}
		return result;
	}

	@Override
	public Product<?> fetch(String orgNo, String remoteKey) throws Exception {
		Article a = connector.client(orgNo).getArticleByArticleNo(remoteKey);
		if (a == null) throw new Exception("Article " + remoteKey + " not found in Fortnox");
		return toCanonical(a);
	}

	@Override
	public SyncLink create(String orgNo, Product<?> obj) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		Article created = client.setArticle(toFortnox(obj), true);
		if (created == null || empty(created.getArticleNumber()) == null) throw new Exception("Fortnox didn't return an article number");
		setSalesPrice(client, orgNo, created.getArticleNumber(), obj.getSalesPrice(), created.getSalesPrice());
		return new SyncLink(null, created.getArticleNumber());
	}

	@Override
	public SyncLink update(String orgNo, Product<?> obj, SyncLink link) throws Exception {
		FortnoxClient3 client = connector.client(orgNo);
		Article a = toFortnox(obj);
		a.setArticleNumber(link.getRemoteKey());
		Article updated = client.setArticle(a, false);
		setSalesPrice(client, orgNo, link.getRemoteKey(), obj.getSalesPrice(), updated != null ? updated.getSalesPrice() : null);
		return link;
	}

	/** Writes the sales price to the preselected price list, unless it's already the price. */
	private void setSalesPrice(FortnoxClient3 client, String orgNo, String articleNo, Double price, Double current) throws Exception {
		if (price == null || (current != null && Math.abs(current - price) < 0.005)) return;
		String list = defaultPriceList(client, orgNo);
		Price p = new Price();
		p.setPriceList(list);
		p.setArticleNumber(articleNo);
		p.setFromQuantity(0.0);
		p.setPrice(price);
		boolean exists = client.getPrice(list, articleNo, BigDecimal.ZERO) != null;
		client.setPrice(p, !exists);
	}

	private String defaultPriceList(FortnoxClient3 client, String orgNo) throws Exception {
		String code = defaultPriceLists.get(orgNo);
		if (code != null) return code;
		code = "A";
		PriceLists lists = client.getPriceLists();
		if (lists != null && lists.getPriceListSubset() != null) {
			for (PriceListSubset s : lists.getPriceListSubset()) {
				if (Boolean.TRUE.equals(s.getPreSelected()) && empty(s.getCode()) != null) code = s.getCode().trim();
			}
		}
		defaultPriceLists.put(orgNo, code);
		return code;
	}

	// ------------------------------------------------------------------

	public static Product<?> toCanonical(Article a) {
		Product<Article> p = new Product<Article>();
		p.setNativeProduct(a);
		p.setKey(empty(a.getArticleNumber()));
		p.setName(empty(a.getDescription()));
		p.setUnit(empty(a.getUnit()));
		p.setUpc(empty(a.getEan()));
		p.setBrand(empty(a.getManufacturer()));
		p.setActive(!Boolean.FALSE.equals(a.getActive()));
		if (a.getType() != null) p.setService(TYPE_SERVICE.equalsIgnoreCase(a.getType().trim()));
		if (a.getWeight() != null) p.setWeight(a.getWeight() / 1000.0);
		if (a.getWidth() != null) p.setWidth(a.getWidth());
		if (a.getHeight() != null) p.setHeight(a.getHeight());
		if (a.getDepth() != null) p.setLength(a.getDepth());
		p.setSalesAccount(account(a.getSalesAccount()));
		p.setPurchaseAccount(account(a.getPurchaseAccount()));
		p.setSalesPrice(a.getSalesPrice());
		ProductSyncProfile.setVatRate(p, a.getVat());
		String supplierNo = empty(a.getSupplierNumber());
		if (supplierNo != null || (a.getPurchasePrice() != null && a.getPurchasePrice() != 0)) {
			ProductSupplier s = new ProductSupplier(supplierNo, null);
			s.setSupplierName(empty(a.getSupplierName()));
			s.setPurchasePrice(a.getPurchasePrice());
			s.setPreferred(true);
			p.addSupplier(s);
		}
		return p;
	}

	public static Article toFortnox(Product<?> p) throws Exception {
		Article a = new Article();
		String name = empty(p.getName());
		if (name == null) throw new Exception("The article has no name");
		a.setArticleNumber(empty(p.getKey()));
		a.setDescription(name);
		a.setUnit(orClear(p.getUnit()));
		a.setEan(orClear(p.getUpc()));
		a.setManufacturer(orClear(p.getBrand()));
		a.setActive(p.isActive());
		if (p.getService() != null) a.setType(p.getService() ? TYPE_SERVICE : TYPE_STOCK);
		a.setWeight(p.getWeight() != 0 ? p.getWeight() * 1000.0 : null);
		a.setWidth(p.getWidth() != 0 ? (int) Math.round(p.getWidth()) : null);
		a.setHeight(p.getHeight() != 0 ? (int) Math.round(p.getHeight()) : null);
		a.setDepth(p.getLength() != 0 ? (int) Math.round(p.getLength()) : null);
		a.setSalesAccount(accountNo(p.getSalesAccount(), "sales"));
		a.setPurchaseAccount(accountNo(p.getPurchaseAccount(), "purchase"));
		a.setVat(ProductSyncProfile.vatRate(p));
		ProductSupplier s = p.preferredSupplier();
		a.setSupplierNumber(s != null && empty(s.getSupplierKey()) != null ? s.getSupplierKey().trim() : "");
		a.setPurchasePrice(s != null && s.getPurchasePrice() != null ? s.getPurchasePrice() : 0.0);
		return a;
	}

	private static String account(Integer acct) {
		return acct != null && acct != 0 ? String.valueOf(acct) : null;
	}

	private static Integer accountNo(String acct, String what) throws Exception {
		String a = empty(acct);
		if (a == null) return null;
		if (!a.matches("\\d{4}")) throw new Exception("The " + what + " account isn't an account number: " + a);
		return Integer.valueOf(a);
	}

	static String empty(String s) {
		return s != null && !s.trim().isEmpty() ? s.trim() : null;
	}

	/** "" clears the field in Fortnox, null would leave it. */
	static String orClear(String s) {
		return s != null ? s.trim() : "";
	}

}
