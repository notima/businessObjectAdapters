package org.notima.generic.adempiere.factory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.notima.generic.adempiere.AdempiereBusinessPartner;
import org.notima.generic.adempiere.AdempiereContact;
import org.notima.generic.adempiere.AdempiereDunningRun;
import org.notima.generic.adempiere.AdempiereFactoringReservation;
import org.notima.generic.adempiere.AdempiereInvoice;
import org.notima.generic.adempiere.AdempiereLocation;
import org.notima.generic.adempiere.AdempiereOrder;
import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BasicFactoringReservation;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.CustomObject;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.Person;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.PriceListLine;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.ProductInfo;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;

public class AdempiereJdbcFactory extends BasicBusinessObjectFactory {

	public final static String SYSTEMNAME = "Adempiere";
	
	public final static String 	BRAND_LIST = "brandlist";
	public final static String	UNIT_LIST = "unitlist";
	
	private static Logger logger = Logger.getLogger(AdempiereJdbcFactory.class.getName());
	
	private Connection m_conn;
	private int			adClientId;
	private int			adOrgId;
	private int			userId = 0;
	
	public AdempiereJdbcFactory(Object ds, int clientId, int orgId) throws Exception {
		
		if (ds==null || !(ds instanceof DataSource)) {
			logger.warning("No datasource supplied");
			if (ds!=null) {
				logger.warning("Don't know what to do with a " + ds.getClass().getCanonicalName());
			}
			return;
		}
		
		m_conn = ((DataSource)ds).getConnection();
		adClientId = clientId;
		adOrgId = orgId;
		
	}
	
	public AdempiereJdbcFactory(String jdbcUrl, String user, String pass, int clientId, int orgId) throws Exception {
		Class.forName("org.postgresql.Driver");
		m_conn = DriverManager.getConnection(jdbcUrl, user, pass);
		adClientId = clientId;
		adOrgId = orgId; 
	}
	
	public void closeDatabase() throws SQLException {
		if (m_conn!=null && !m_conn.isClosed()) {
			m_conn.close();
		}
	}

	public int getADClientID() {
		return adClientId;
	}
	
	public int getADOrgId() {
		return adOrgId;
	}
	
	public boolean isConnected() throws Exception {
		return(m_conn!=null && !m_conn.isClosed());
	}

	public void destroy() throws Exception {
		closeDatabase();
	}
	
	public BusinessPartner lookupBusinessPartner(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<BusinessPartner> lookupAllBusinessPartners() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}
	public DunningRun lookupDunningRun(String key) throws Exception{
		
		int dunningRunId = Integer.parseInt(key);
		DunningRun dun = AdempiereDunningRun.load(dunningRunId, m_conn);
		
		return dun;	
	}

	public Invoice lookupInvoice(String key) throws Exception {
		
		Invoice invoice = AdempiereInvoice.load(key, adClientId, adOrgId, m_conn);
		
		return invoice;
	}

	public Order lookupOrder(String key) throws Exception {
		
		// Convert to int
		int orderId = Integer.parseInt(key);
		Order order = AdempiereOrder.load(orderId, m_conn);
		
		return order;
	}

	public List<Product> lookupProductByName(String name) throws Exception {
		List<AdempiereProduct> result = AdempiereProduct.findByName(m_conn, adClientId, name);
		if (result.size()>0) {
			List<Product> plist = new ArrayList<Product>();
			for (AdempiereProduct p : result) {
				plist.add(convert(p));
			}
			return (plist);
		}
		return null;
	}
	
	public Product lookupProduct(String key) throws Exception {
		
		return null;
	}

	public Product lookupProductByEan(String ean) throws Exception {

		List<AdempiereProduct> result = AdempiereProduct.findByEan(m_conn, adClientId, ean);
		if (result.size()>0) {
			return (convert(result.get(0)));
		}
		return null;
	}

	public Product lookupRoundingProduct() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public Tax lookupTax(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public PaymentTerm lookupPaymentTerm(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public FactoringReservation lookupFactoringReservation(String key)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FactoringReservation> lookupFactoringReservationForOrder(
			String orderKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	public List<FactoringReservation> lookupFactoringReservationForInvoice(
			String invoiceKey) throws Exception {
		return null;
	}

	public Object persist(Object o) throws Exception {
		if (o instanceof CustomObject) {
			persistCustomObject((CustomObject)o);
		}
		
		if (o instanceof Product) {
			persistProduct(convert((Product)o));
			return o;
		}
		
		if (o instanceof ProductInfo) {
			ProductInfo pf = (ProductInfo)o;
			return persistProduct(convert(pf.getProduct()));
			//TODO: Persist price and other info that comes with ProductInfo
		}
		
		if (o instanceof AdempiereProduct) {
			return persistProduct((AdempiereProduct)o);
		}
		
		if (o instanceof AdempierePrice) {
			AdempierePrice pp = (AdempierePrice)o;
			pp.persist(m_conn);
			return pp;
		}
		
		if (o instanceof AdempiereBusinessPartner) {
			AdempiereBusinessPartner c = (AdempiereBusinessPartner)o;
			c.persist(m_conn);
			return c;
		}
		
		if (o instanceof AdempiereContact) {
			AdempiereContact c = (AdempiereContact)o;
			c.persist(m_conn);
			return c;
		}
		
		if (o instanceof ProductCategory) {
			return persistProductCategory(convert((ProductCategory)o));
		}
		
		if (o instanceof AdempiereLocation) {
			AdempiereLocation loc = (AdempiereLocation)o;
			loc.persist(m_conn);
			return loc;
		}
		
		if (o instanceof AdempiereFactoringReservation) {
			AdempiereFactoringReservation fr = (AdempiereFactoringReservation)o;
			fr.persist(m_conn);
			return fr;
		}
		
		return null;
	}

	private AdempiereProduct persistProduct(AdempiereProduct p) throws Exception {
		p.persist(m_conn);
		return p;
	}
	
	private AdempiereProductCategory persistProductCategory(AdempiereProductCategory p) throws Exception {
		p.persist(m_conn);
		return p;
	}
	
	/**
	 * A custom object is persisted by using table = customType
	 * 
	 * @param p
	 * @return
	 * @throws Exception
	 */
	private CustomObject persistCustomObject(CustomObject p) throws Exception {
		
		String query = "select " + p.getCustomType()+ "_id" + " from " 
				+ p.getCustomType() + " where value=? and ad_client_id=?";
		PreparedStatement ps = m_conn.prepareStatement(query);
		ps.setString(1, p.getKey());
		ps.setInt(2, adClientId);
		ResultSet rs = ps.executeQuery();
		int ID = 0;
		if (rs.next()) {
			ID = rs.getInt(1);
		}
		rs.close();
		ps.close();
		
		if (ID>0) {
			// Update
			ps = m_conn.prepareStatement(
					"update " + p.getCustomType() + " set name=? where " + p.getCustomType() + "_id=?"
					);
			ps.setString(1,  p.getValue());
			ps.setInt(2, ID);
			ps.executeUpdate();
			ps.close();
		} else {
			// Insert
			String insert = "insert into " + p.getCustomType() + " (" + p.getCustomType() + "_id, ad_client_id, ad_org_id, createdby, updatedby, value, name, " + 
						    p.getCustomType() + "_uu) values (nextval('" + p.getCustomType() + "_sq'), ?,?,?,?,?,?,uuid_generate_v4())";
			ps = m_conn.prepareStatement(insert);
			int c=1;
			ps.setInt(c++, adClientId);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, userId);
			ps.setInt(c++, userId);
			ps.setString(c++, p.getKey());
			ps.setString(c++, p.getValue());
			ps.executeUpdate();
			ps.close();
		}
		
		return p;
	}
	
	public Product convert(AdempiereProduct src) {
		Product dst = new Product();
		dst.setKey(src.getProductNo());
		dst.setName(src.getName());
		dst.setPackageInfo(src.getPackInfo());
		dst.setUpc(src.getEan());
		dst.setBrand(src.getBrand());
		dst.setUnit(src.getUomSymbol());
		return dst;
	}

	public AdempiereLocation convert(Location src) {
		AdempiereLocation dst = new AdempiereLocation();
		
		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());
		dst.setName(src.getName());
		dst.setActive(true);
		dst.setAddress1(src.getAddress1());
		dst.setAddress2(src.getAddress2());
		dst.setAddress3(src.getAddress3());
		dst.setAddress4(src.getAddress4());
		dst.setCity(src.getCity());
		dst.setCountryCode(src.getCountryCode());
		dst.setPostal(src.getPostal());
		dst.setPhone(src.getPhone());
		
		return dst;
	}
	
	/**
	 * Converts business partner
	 * 
	 * @param src
	 * @return
	 */
	public AdempiereBusinessPartner convert(BusinessPartner src) {
		AdempiereBusinessPartner dst = new AdempiereBusinessPartner();

		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());
		dst.setName(src.getName());
		dst.setCustomerNo(src.getIdentityNo());
		dst.setAdLanguage(src.getLanguage());
		dst.setTaxId(src.getTaxId());
		
		return dst;
	}
	
	/**
	 * Converts business partner
	 * 
	 * @param src
	 * @return
	 */
	public AdempiereContact convert(Person src) {
		AdempiereContact dst = new AdempiereContact();

		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());
		StringBuffer nameBuf = new StringBuffer();
		if (src.getFirstName()!=null && src.getFirstName().trim().length()>0) {
			nameBuf.append(src.getFirstName());
		}
		if (src.getLastName()!=null && src.getLastName().trim().length()>0) {
			if (nameBuf.length()>0)
				nameBuf.append(" ");
			nameBuf.append(src.getLastName());
		}
		dst.setName(nameBuf.toString());
		dst.setEmail(src.getEmail());
		dst.setPhone(src.getPhone());
		dst.setUser(Integer.toString(src.getPersonId()));
		
		return dst;
	}

	/**
	 * Only maps amount and their reservation id, the rest of the fields
	 * must be mapped in another way.
	 * 
	 * @param src
	 * @return
	 */
	public AdempiereFactoringReservation convert(BasicFactoringReservation src) {
		
		AdempiereFactoringReservation dst = new AdempiereFactoringReservation();
		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());

		dst.setAmount(src.getAmount());
		dst.setSupplierReference(src.getTheirReservationId());
		
		return dst;
		
	}
	
	public AdempiereProduct convert(Product src) {
		AdempiereProduct dst = new AdempiereProduct();
		dst.setProductNo(src.getKeyReference()!=null && src.getKeyReference().trim().length()>0 ? src.getKeyReference() : src.getKey());
		dst.setName(src.getName());
		dst.setPackInfo(src.getPackageInfo());
		dst.setEan(src.getUpc());
		dst.setBrand(src.getBrand());
		dst.setUomSymbol(src.getUnit());
		// TODO: Unitmapping
		dst.setcUomId(100);
		// TODO: Product category mapping
		dst.setmProductCategoryId(1000000);
		// TODO: Tax category mapping
		dst.setTaxCategoryId(1000000);
		
		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());
		return dst;
	}

	public AdempiereProductCategory convert(ProductCategory src) {
		AdempiereProductCategory dst = new AdempiereProductCategory();
		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());
		dst.setName(src.getName());
		dst.setCategoryNo(src.getKey());
		dst.setParentCategoryNo(src.getParentKey());
		return dst;
	}
	
	public AdempierePrice convert(PriceListLine src) {
		AdempierePrice dst = new AdempierePrice();

		dst.setAdClientId(getADClientID());
		dst.setAdOrgId(getADOrgId());
		dst.setPriceLimit(src.getLimitPrice());
		dst.setPriceList(src.getListPrice());
		dst.setPriceStd(src.getStdPrice());
		
		return dst;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public Map lookupList(String listName) throws Exception {
		if (BRAND_LIST.equals(listName)) {
			return lookupBrandList();
		}
		return null;
	}
	
	private Map<Integer, String> lookupBrandList() throws SQLException {
		String query = "select xt_brand_id, name from xt_brand where ad_client_id=? order by name";
		PreparedStatement ps = m_conn.prepareStatement(query);
		ps.setInt(1, adClientId);
		Map<Integer,String> result = new TreeMap<Integer,String>();
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.put(rs.getInt(1), rs.getString(2));
		}
		rs.close();
		ps.close();
		return result;
	}

	@Override
	public Object getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupNativeInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persistNativeInvoice(Object invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupNativeOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persistNativeOrder(Object order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PriceList lookupPriceForProduct(String productKey, String currency,
			Boolean salesPriceList) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List lookupProductCategory(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List lookupBusinessPartners(int maxCount, boolean customers, boolean suppliers) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSystemName() {
		return SYSTEMNAME;
	}

	@Override
	public BusinessPartnerList listTenants() {

		BusinessPartnerList bpl = new BusinessPartnerList();
		List<BusinessPartner> list = new ArrayList<BusinessPartner>();
		bpl.setBusinessPartner(list);

		try {
			String query = "select ad_client_id, value, name from ad_client where ad_client_id>=1000000 order by value";

			BusinessPartner bp = null;
			PreparedStatement ps = m_conn.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				bp = new BusinessPartner();
				bp.setIdentityNo(Integer.toString(rs.getInt(1)));
				bp.setName(rs.getString(3));
				list.add(bp);
			}
			rs.close();
			ps.close();
			
		} catch (Exception ee) {

			ee.printStackTrace();
			
		}
		
		return bpl;
	}
	
}
