package org.notima.generic.adempiere.factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdempiereProduct {
	
	private int		adClientId;
	private int		adOrgId;
	private int		mProductId;
	private String	productNo;
	private String	name;
	private String	ean;
	private String	brand;
	private String	packInfo;
	private String	uomSymbol;
	private int		cUomId;
	private int		mProductCategoryId;
	private int		taxCategoryId;
	
	private static String selectSql = 
			"select p.ad_client_id, p.m_product_id, p.ad_org_id, p.m_product_category_id, p.c_taxcategory_id, p.value, p.name, p.upc, p.packinfo, " + 
			"br.name as brand, u.uomsymbol as unit, p.c_uom_id " +
			"from " +
			"m_product p " +
			"left join xt_brand br on (p.xt_brand_id=br.xt_brand_id) " +
			"left join c_uom u on (p.c_uom_id=u.c_uom_id) ";
	
	private static String updateSql = 
			"update m_product set upc=?, name=?, c_uom_id=?, m_product_category_id=?, c_taxcategory_id=?, ad_org_id=? where value=? and ad_client_id=?";
	
	private static String insertSql = 
			"insert into m_product " + 
					"(m_product_id, createdby, updatedby, upc, name, c_uom_id, m_product_category_id, c_taxcategory_id, " + 
					"ad_org_id, value, ad_client_id, m_product_uu) " + 
					"values (nextval('m_product_sq'),0,0,?,?,?,?,?,?,?,?, uuid_generate_v4())";
	
	public AdempiereProduct(ResultSet rs) throws SQLException {
		int c = 1;
		adClientId = rs.getInt(c++);
		mProductId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		mProductCategoryId = rs.getInt(c++);
		taxCategoryId = rs.getInt(c++);
		productNo = rs.getString(c++);
		name = rs.getString(c++);
		ean = rs.getString(c++);
		packInfo = rs.getString(c++);
		brand = rs.getString(c++);
		uomSymbol = rs.getString(c++);
		cUomId = rs.getInt(c++);
	}
	
	public AdempiereProduct() {
	}

	public int persist(Connection conn) throws SQLException {
		
		// Start transaction
		conn.setAutoCommit(false);

		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select m_product_id from m_product where ad_client_id=? and value=?");
			ps.setInt(1, adClientId);
			ps.setString(2, productNo);
			ResultSet rs = ps.executeQuery();
			int productId = 0;
			if (rs.next()) {
				productId = rs.getInt(1);
			}
			mProductId=productId;
			rs.close();
			ps.close();
			int c = 1;
			ps = conn.prepareStatement(productId>0 ? updateSql : insertSql);
			ps.setString(c++, ean);
			ps.setString(c++, name);
			ps.setInt(c++, cUomId);
			ps.setInt(c++, mProductCategoryId);
			ps.setInt(c++, taxCategoryId);
			ps.setInt(c++, adOrgId);
			ps.setString(c++, productNo);
			ps.setInt(c++, adClientId);
			int result = ps.executeUpdate();
			ps.close();
			
			// If insert find last id
			// If new, get last sequence
			if (productId==0) {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery("select currval('m_product_sq')");
				if (rs.next())
					productId = rs.getInt(1);
				rs.close();
				stmt.close();
				mProductId = productId;
				
				// Insert accounting
				String qinsert = "insert into m_product_acct " + 
				" (m_product_id, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" p_revenue_acct, p_expense_acct, p_asset_acct, p_purchasepricevariance_acct, p_invoicepricevariance_acct, " +
				" p_cogs_acct, p_tradediscountrec_acct, p_tradediscountgrant_acct, p_inventoryclearing_acct, p_costadjustment_acct, " + 
				" p_wip_acct, p_methodchangevariance_acct, p_usagevariance_acct, p_ratevariance_acct, p_mixvariance_acct, " + 
				" p_floorstock_acct, p_costofproduction_acct, p_labor_acct, p_burden_acct, p_outsideprocessing_acct, p_overhead_acct, " + 
				" p_scrap_acct, p_averagecostvariance_acct, p_landedcostclearing_acct, m_product_acct_uu) " +
				" select " +
				" ?, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" p_revenue_acct, p_expense_acct, p_asset_acct, p_purchasepricevariance_acct, p_invoicepricevariance_acct, " +
				" p_cogs_acct, p_tradediscountrec_acct, p_tradediscountgrant_acct, p_inventoryclearing_acct, p_costadjustment_acct, " + 
				" p_wip_acct, p_methodchangevariance_acct, p_usagevariance_acct, p_ratevariance_acct, p_mixvariance_acct, " + 
				" p_floorstock_acct, p_costofproduction_acct, p_labor_acct, p_burden_acct, p_outsideprocessing_acct, p_overhead_acct, " + 
				" p_scrap_acct, p_averagecostvariance_acct, p_landedcostclearing_acct, " +
				" uuid_generate_v4()" +
				" from m_product_category_acct where m_product_category_id=?";
				
				ps = conn.prepareStatement(qinsert);
				ps.setInt(1, productId);
				ps.setInt(2, mProductCategoryId);
				ps.executeUpdate();
				ps.close();
				
			}

			conn.commit();
			
			return result;
		} catch (SQLException se) {
			conn.rollback();
			throw se;
		} finally {
			conn.setAutoCommit(true);
		}
	}
	
	public String getProductNo() {
		return productNo;
	}
	public void setProductNo(String productNo) {
		this.productNo = productNo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getEan() {
		return ean;
	}
	public void setEan(String ean) {
		this.ean = ean;
	}
	public String getBrand() {
		return brand;
	}
	public void setBrand(String brand) {
		this.brand = brand;
	}
	public String getPackInfo() {
		return packInfo;
	}
	public void setPackInfo(String packInfo) {
		this.packInfo = packInfo;
	}
	
	public int getcUomId() {
		return cUomId;
	}

	public void setcUomId(int cUomId) {
		this.cUomId = cUomId;
	}

	public String getUomSymbol() {
		return uomSymbol;
	}

	public void setUomSymbol(String uomSymbol) {
		this.uomSymbol = uomSymbol;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (productNo!=null)
			buf.append(productNo);
		
		if (name!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(name);
		}
		
		if (brand!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(brand);
		}
		
		if (packInfo!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(packInfo);
		}
		
		if (uomSymbol!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(uomSymbol);
		}
		return buf.toString();
		
	}
	
	public static List<AdempiereProduct> findByEan(Connection conn, int clientId, String ean) throws SQLException {

		List<AdempiereProduct> result = new ArrayList<AdempiereProduct>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where p.upc=? and p.AD_Client_ID=?");
		ps.setString(1, ean);
		ps.setInt(2, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereProduct(rs));
		}
		rs.close();
		ps.close();
		
		return result;
	}
	
	public static List<AdempiereProduct> findByName(Connection conn, int clientId, String name) throws SQLException {

		List<AdempiereProduct> result = new ArrayList<AdempiereProduct>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where (p.value=? or p.name ilike ? or br.name ilike ?) and p.AD_Client_ID=?");
		ps.setString(1, name);
		ps.setString(2, "%"+name+"%");
		ps.setString(3, "%"+name+"%");
		ps.setInt(4, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereProduct(rs));
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

	public int getmProductId() {
		return mProductId;
	}

	public void setmProductId(int mProductId) {
		this.mProductId = mProductId;
	}

	public int getmProductCategoryId() {
		return mProductCategoryId;
	}

	public void setmProductCategoryId(int mProductCategoryId) {
		this.mProductCategoryId = mProductCategoryId;
	}

	public int getTaxCategoryId() {
		return taxCategoryId;
	}

	public void setTaxCategoryId(int taxCategoryId) {
		this.taxCategoryId = taxCategoryId;
	}
	
	
}
