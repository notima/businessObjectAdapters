package org.notima.generic.adempiere.factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdempiereProductCategory {
	
	private int		adClientId;
	private int		adOrgId;
	private String	categoryNo;
	private String	parentCategoryNo;
	private String	name;
	private int		categoryId;
	private int		parentCategoryId;
	
	
	private static String selectSql = 
			"select pc.ad_client_id, pc.ad_org_id, pc.m_product_category_id, pc.m_product_category_parent_id, " +
					"(select pp.value from pp.m_product_category where pp.m_product_category_id=pc.m_product_category_parent_id) as parentvalue, " +
					"pc.name, pc.value " + 
			"from " +
			"m_product_category pc ";
	
	private static String updateSql = 
			"update m_product_category set name=?, ad_org_id=? where value=? and ad_client_id=?";
	
	private static String insertSql = 
			"insert into m_product_category " + 
					"(m_product_category_id, createdby, updatedby, name, " + 
					"ad_org_id, value, ad_client_id, plannedmargin, m_product_category_uu) " + 
					"values (nextval('m_product_category_sq'),1,1,?,?,?,?,0, uuid_generate_v4())";
	
	public AdempiereProductCategory(ResultSet rs) throws SQLException {
		int c = 1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		categoryId = rs.getInt(c++);
		parentCategoryId = rs.getInt(c++);
		parentCategoryNo = rs.getString(c++);
		name = rs.getString(c++);
		categoryNo = rs.getString(c++);
	}
	
	public AdempiereProductCategory() {
	}

	public int persist(Connection conn) throws SQLException {
		// Start transaction
		conn.setAutoCommit(false);
		
		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select m_product_category_id from m_product_category where ad_client_id=? and value=?");
			ps.setInt(1, adClientId);
			ps.setString(2, categoryNo);
			ResultSet rs = ps.executeQuery();
			int productCategoryId = 0;
			if (rs.next()) {
				productCategoryId = rs.getInt(1);
			}
			rs.close();
			ps.close();
			int c = 1;
			ps = conn.prepareStatement(productCategoryId>0 ? updateSql : insertSql);
			ps.setString(c++, name);
			ps.setInt(c++, adOrgId);
			ps.setString(c++, categoryNo);
			ps.setInt(c++, adClientId);
			int result = ps.executeUpdate();
			ps.close();
			
			// If new, get last sequence
			if (productCategoryId==0) {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery("select currval('m_product_category_sq')");
				if (rs.next())
					productCategoryId = rs.getInt(1);
				rs.close();
				stmt.close();
				
				// Get default costing method and costing level
				stmt = conn.createStatement();
				rs = stmt.executeQuery("select costingmethod, costinglevel from c_acctschema where isactive='Y' and ad_client_id=" + adClientId);
				String costingMethod = null, costingLevel = null;
				if (rs.next()) {
					costingMethod = rs.getString(1);
					costingLevel = rs.getString(2);
				}
				rs.close();
				stmt.close();
				
				// Insert accounting
				String qinsert = "insert into m_product_category_acct " + 
				" (m_product_category_id, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" p_revenue_acct, p_expense_acct, p_asset_acct, p_purchasepricevariance_acct, p_invoicepricevariance_acct, " +
				" p_cogs_acct, p_tradediscountrec_acct, p_tradediscountgrant_acct, " +
				" costingmethod, costinglevel, " +
				" p_inventoryclearing_acct, p_costadjustment_acct, " + 
				" p_wip_acct, p_methodchangevariance_acct, p_usagevariance_acct, p_ratevariance_acct, p_mixvariance_acct, " + 
				" p_floorstock_acct, p_costofproduction_acct, p_labor_acct, p_burden_acct, p_outsideprocessing_acct, p_overhead_acct, " + 
				" p_scrap_acct, p_averagecostvariance_acct, p_landedcostclearing_acct, " + 
				" m_product_category_acct_uu) " +
				" select " +
				" ?, c_acctschema_id, ad_client_id, ad_org_id, isactive, createdby, updatedby, " +
				" p_revenue_acct, p_expense_acct, p_asset_acct, p_purchasepricevariance_acct, p_invoicepricevariance_acct, " +
				" p_cogs_acct, p_tradediscountrec_acct, p_tradediscountgrant_acct, " +
				" ?, ?, " +
				" p_inventoryclearing_acct, p_costadjustment_acct, " + 
				" p_wip_acct, p_methodchangevariance_acct, p_usagevariance_acct, p_ratevariance_acct, p_mixvariance_acct, " + 
				" p_floorstock_acct, p_costofproduction_acct, p_labor_acct, p_burden_acct, p_outsideprocessing_acct, p_overhead_acct, " + 
				" p_scrap_acct, p_averagecostvariance_acct, p_landedcostclearing_acct, " +
				" uuid_generate_v4()" +
				" from c_acctschema_default where ad_client_id=? and isactive='Y'";
				
				ps = conn.prepareStatement(qinsert);
				ps.setInt(1, productCategoryId);
				ps.setString(2, costingMethod);
				ps.setString(3, costingLevel);
				ps.setInt(4, adClientId);
				ps.executeUpdate();
				ps.close();
				
			}
			
			// Set parent category id
			ps = conn.prepareStatement("select m_product_category_id from m_product_category where value=? and ad_client_id=?");
			ps.setString(1, parentCategoryNo);
			ps.setInt(2, adClientId);
			rs = ps.executeQuery();
			int parentId = 0;
			if (rs.next()) {
				parentId = rs.getInt(1);
			}
			rs.close();
			ps.close();
			if (parentId>0) {
				ps = conn.prepareStatement("update m_product_category set m_product_category_parent_id=? where m_product_category_id=?");
				ps.setInt(1, parentId);
				ps.setInt(2, productCategoryId);
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
	
	public String getCategoryNo() {
		return categoryNo;
	}
	public void setCategoryNo(String categoryNo) {
		this.categoryNo = categoryNo;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}


	public String toString() {
		StringBuffer buf = new StringBuffer();
		if (categoryNo!=null)
			buf.append(categoryNo);
		
		if (name!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(name);
		}
		
		return buf.toString();
		
	}
	
	public static List<AdempiereProductCategory> findByName(Connection conn, int clientId, String name) throws SQLException {

		List<AdempiereProductCategory> result = new ArrayList<AdempiereProductCategory>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where (p.value=? or p.name ilike ? or br.name ilike ?) and p.AD_Client_ID=?");
		ps.setString(1, name);
		ps.setString(2, "%"+name+"%");
		ps.setString(3, "%"+name+"%");
		ps.setInt(4, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereProductCategory(rs));
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

	public String getParentCategoryNo() {
		return parentCategoryNo;
	}

	public void setParentCategoryNo(String parentCategoryNo) {
		this.parentCategoryNo = parentCategoryNo;
	}

	public int getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(int categoryId) {
		this.categoryId = categoryId;
	}

	public int getParentCategoryId() {
		return parentCategoryId;
	}

	public void setParentCategoryId(int parentCategoryId) {
		this.parentCategoryId = parentCategoryId;
	}
	
	
	
}
