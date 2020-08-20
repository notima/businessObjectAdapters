package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.KeyValue;

public class AdempiereBusinessPartner {
	
	private int		adClientId;
	private int		adOrgId;
	private int		partnerId;
	private String	customerNo;
	private String	name;
	private String	taxId;
	private int		partnerGroupId;
	private boolean	isCustomer = true;
	private boolean	isVendor;
	private boolean	isProspect;
	private String  adLanguage;
	private int		priceListId;
	private int		poPriceListId;
	private int		dunningId;
	
	private static String selectSql = 
			"select bp.ad_client_id, bp.ad_org_id, bp.name, bp.taxid, c_bp_group_id, " +
			"iscustomer, isvendor, isprospect, ad_language, m_pricelist_id, po_pricelist_id, c_dunning_id, " + 
			"value, bp.c_bpartner_id " +
			"from " +
			"c_bpartner bp ";
	
	private static String updateSql = 
			"update c_bpartner set ad_client_id=?, ad_org_id=?, name=?, taxid=?, c_bp_group_id=?, " +
					"iscustomer=?, isvendor=?, isprospect=?, ad_language=?, m_pricelist_id=?, po_pricelist_id=?, c_dunning_id=?, " +
					"value=? where c_bpartner_id=?";
	
	private static String insertSql = 
			"insert into c_bpartner " + 
					"(ad_client_id, ad_org_id, createdby, updatedby, name, taxid, c_bp_group_id, " +
					"iscustomer, isvendor, isprospect, ad_language, m_pricelist_id, po_pricelist_id, c_dunning_id, " +
					" value, " +
					"c_bpartner_id, c_bpartner_uu) " + 
					"values (?,?, 0,0,?,?,?,?,?,?,?,?,?,?,?,nextval('c_bpartner_sq'), uuid_generate_v4())";
	
	public AdempiereBusinessPartner(ResultSet rs) throws SQLException {
		int c = 1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		name = rs.getString(c++);
		taxId = rs.getString(c++);
		partnerGroupId = rs.getInt(c++);
		isCustomer = "Y".equalsIgnoreCase(rs.getString(c++));
		isVendor = "Y".equalsIgnoreCase(rs.getString(c++));
		isProspect = "Y".equalsIgnoreCase(rs.getString(c++));
		adLanguage = rs.getString(c++);
		priceListId = rs.getInt(c++);
		poPriceListId = rs.getInt(c++);
		dunningId = rs.getInt(c++);
		customerNo = rs.getString(c++);
		partnerId = rs.getInt(c++);
	}
	
	public static BusinessPartner load(int bpartnerId, Connection conn) throws Exception {
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where c_bpartner_id=?");
		ps.setInt(1, bpartnerId);
		ResultSet rs = ps.executeQuery();
		AdempiereBusinessPartner abp = null;
		if (rs.next()) {
			abp = new AdempiereBusinessPartner(rs);
		}
		rs.close();
		ps.close();

		if (abp==null)
			return null;

		BusinessPartner bp = new BusinessPartner();
		// Map
		bp.setName(abp.getName());
		bp.setbPartnerId(bpartnerId);
		bp.setIdentityNo(abp.getCustomerNo());
		bp.setLanguage(abp.getAdLanguage());
		bp.setTaxId(abp.getTaxId());
		// Create attributes
		List<KeyValue> attrs = new ArrayList<KeyValue>();
		attrs.add(new KeyValue("AD_Client_ID",abp.getAdClientId()));
		attrs.add(new KeyValue("AD_Org_ID",abp.getAdClientId()));
		bp.setAttributes(attrs);

		// Don't lookup location here
		
		return bp;
	}

	/**
	 * Returns Org details as a business partner. Useful for sender details.
	 * 
	 * @param orgNo
	 * @param conn
	 * @return
	 * @throws Exception
	 */
	public static BusinessPartner loadOrgBp(int orgNo, Connection conn) throws Exception {

		String sql = "select o.name, oi.taxid, oi.email, oi.c_location_id from ad_org o " +
					 "left join ad_orginfo oi on (o.ad_org_id=oi.ad_org_id) where o.ad_org_id=?";
		
		BusinessPartner bp = new BusinessPartner();
		
		PreparedStatement ps = conn.prepareStatement(sql);
		
		ps.setInt(1, orgNo);
		
		ResultSet rs = ps.executeQuery();
		String email = null;
		int locationId = 0;
		int c=1;
		if (rs.next()) {
			bp.setCompany(true);
			bp.setName(rs.getString(c++));
			bp.setTaxId(rs.getString(c++));
			email = rs.getString(c++);
			locationId = rs.getInt(c++);
		}

		rs.close();
		ps.close();

		AdempiereLocation loc = AdempiereLocation.findByCLocation(conn, locationId);
		bp.setAddressOfficial(AdempiereLocation.convert(loc));
		
		return bp;
	}
	
	public AdempiereBusinessPartner() {
	}
	
	public int persist(Connection conn) throws SQLException {
		
		// Start transaction
		conn.setAutoCommit(false);

		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select c_bpartner_id from c_bpartner where ad_client_id=? and value=?");
			ps.setInt(1, adClientId);
			ps.setString(2,  customerNo);
			ResultSet rs = ps.executeQuery();
			int tmpPartnerId = 0;
			if (rs.next()) {
				tmpPartnerId = rs.getInt(1);
			}
			partnerId=tmpPartnerId;
			rs.close();
			ps.close();
			int c = 1;
			ps = conn.prepareStatement(tmpPartnerId>0 ? updateSql : insertSql);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, adClientId);
			ps.setString(c++, name);
			ps.setString(c++, taxId);
			ps.setInt(c++, partnerGroupId);
			ps.setString(c++, isCustomer ? "Y" : "N");
			ps.setString(c++, isVendor ? "Y" : "N");
			ps.setString(c++, isProspect ? "Y" : "N");
			ps.setString(c++, adLanguage);
			ps.setInt(c++, priceListId);
			ps.setInt(c++, poPriceListId);
			ps.setInt(c++, dunningId);
			ps.setString(c++, customerNo);
			if (partnerId>0) {
				ps.setInt(c++, partnerId);
			}
			int result = ps.executeUpdate();
			ps.close();
			
			// If insert find last id
			// If new, get last sequence
			if (tmpPartnerId==0) {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery("select currval('c_bpartner_sq')");
				if (rs.next())
					tmpPartnerId = rs.getInt(1);
				rs.close();
				stmt.close();
				partnerId = tmpPartnerId;
				
				// Insert customer accounting
				String qinsert = "insert into c_bp_customer_acct " + 
				" (c_bpartner_id, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" c_receivable_acct, c_prepayment_acct, c_receivable_services_acct, " +
				" c_bp_customer_acct_uu) " +
				" select " +
				" ?, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" c_receivable_acct, c_prepayment_acct, c_receivable_services_acct, " +
				" uuid_generate_v4()" +
				" from c_bp_group_acct where c_bp_group_id=?";
				
				ps = conn.prepareStatement(qinsert);
				ps.setInt(1, tmpPartnerId);
				ps.setInt(2, partnerGroupId);
				ps.executeUpdate();
				ps.close();
				
				// Insert vendor accounting
				qinsert = "insert into c_bp_vendor_acct " + 
				" (c_bpartner_id, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" v_liability_acct, v_liability_services_acct, v_prepayment_acct, " +
				" c_bp_vendor_acct_uu) " +
				" select " +
				" ?, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" v_liability_acct, v_liability_services_acct, v_prepayment_acct, " +
				" uuid_generate_v4()" +
				" from c_bp_group_acct where c_bp_group_id=?";
				
				ps = conn.prepareStatement(qinsert);
				ps.setInt(1, tmpPartnerId);
				ps.setInt(2, partnerGroupId);
				ps.executeUpdate();
				ps.close();
				
				
			}

			conn.commit();
			
			return result;
		} catch (SQLException se) {
			conn.rollback();
			throw se;
		} finally {
			conn.setAutoCommit(false);
		}
	}
	
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		
		if (customerNo!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(customerNo);
		}
		
		if (name!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(name);
		}
		
		return buf.toString();
		
	}
	
	public static List<AdempiereBusinessPartner> findCustomerNo(Connection conn, int clientId, String custNo) throws SQLException {

		List<AdempiereBusinessPartner> result = new ArrayList<AdempiereBusinessPartner>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where value=? and AD_Client_ID=?");
		ps.setString(1, custNo);
		ps.setInt(2, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereBusinessPartner(rs));
		}
		rs.close();
		ps.close();
		
		return result;
	}
	
	public static List<AdempiereBusinessPartner> findByName(Connection conn, int clientId, String name) throws SQLException {

		List<AdempiereBusinessPartner> result = new ArrayList<AdempiereBusinessPartner>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where (value=? or name ilike ?) and AD_Client_ID=?");
		ps.setString(1, name);
		ps.setString(2, "%"+name+"%");
		ps.setInt(3, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereBusinessPartner(rs));
		}
		rs.close();
		ps.close();
		
		return result;
	}

	public int getAdClientId() {
		return adClientId;
	}

	public void setAdClientId(int adClientId) {
		this.adClientId = adClientId;
	}

	public int getAdOrgId() {
		return adOrgId;
	}

	public void setAdOrgId(int adOrgId) {
		this.adOrgId = adOrgId;
	}

	public int getPartnerId() {
		return partnerId;
	}

	public void setPartnerId(int partnerId) {
		this.partnerId = partnerId;
	}

	public String getCustomerNo() {
		return customerNo;
	}

	public void setCustomerNo(String customerNo) {
		this.customerNo = customerNo;
	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}

	public int getPartnerGroupId() {
		return partnerGroupId;
	}

	public void setPartnerGroupId(int partnerGroupId) {
		this.partnerGroupId = partnerGroupId;
	}

	public boolean isCustomer() {
		return isCustomer;
	}

	public void setCustomer(boolean isCustomer) {
		this.isCustomer = isCustomer;
	}

	public boolean isVendor() {
		return isVendor;
	}

	public void setVendor(boolean isVendor) {
		this.isVendor = isVendor;
	}

	public boolean isProspect() {
		return isProspect;
	}

	public void setProspect(boolean isProspect) {
		this.isProspect = isProspect;
	}

	public String getAdLanguage() {
		return adLanguage;
	}

	public void setAdLanguage(String adLanguage) {
		this.adLanguage = adLanguage;
	}

	public int getPriceListId() {
		return priceListId;
	}

	public void setPriceListId(int priceListId) {
		this.priceListId = priceListId;
	}

	public int getPoPriceListId() {
		return poPriceListId;
	}

	public void setPoPriceListId(int poPriceListId) {
		this.poPriceListId = poPriceListId;
	}

	public int getDunningId() {
		return dunningId;
	}

	public void setDunningId(int dunningId) {
		this.dunningId = dunningId;
	}
	
	
	
}
