package org.notima.generic.adempiere.factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AdempierePrice {
	
	private int		adClientId;
	private int		adOrgId;
	private int		priceListVersionId;
	private int		productId;
	private double	priceStd;
	private double	priceList;
	private double	priceLimit;
	
	private static String selectSql = 
			"select pricestd, pricelist, pricelimit, ad_org_id, ad_client_id, m_pricelist_version_id, m_product_id " +
			"from " +
			"m_productprice ";
	
	private static String updateSql = 
			"update m_productprice set pricestd=?, pricelist=?, pricelimit=?, ad_org_id=?, ad_client_id=? " + 
					"where m_pricelist_version_id=? and m_product_id=?";
	
	private static String insertSql = 
			"insert into m_productprice " + 
					"(createdby, updatedby, pricestd, pricelist, pricelimit, " + 
					"ad_org_id, ad_client_id, m_pricelist_version_id, m_product_id, m_productprice_uu) " + 
					"values (0,0,?,?,?,?,?,?,?, uuid_generate_v4())";
	
	public AdempierePrice(ResultSet rs) throws SQLException {
		int c = 1;
		priceStd = rs.getDouble(c++);
		priceList = rs.getDouble(c++);
		priceLimit = rs.getDouble(c++);
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		priceListVersionId = rs.getInt(c++);
		productId = rs.getInt(c++);
	}
	
	public AdempierePrice() {
	}

	public synchronized int persist(Connection conn) throws SQLException {
		// Start transaction
		conn.setAutoCommit(false);
		
		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select m_product_id from m_productprice where m_pricelist_version_id=? and m_product_id=?");
			ps.setInt(1, priceListVersionId);
			ps.setInt(2, productId);
			ResultSet rs = ps.executeQuery();
			int tmpId = 0;
			if (rs.next()) {
				tmpId = rs.getInt(1);
			}
			rs.close();
			ps.close();
			int c = 1;
			ps = conn.prepareStatement(tmpId>0 ? updateSql : insertSql);
			ps.setDouble(c++, priceStd);
			ps.setDouble(c++, priceList);
			ps.setDouble(c++, priceLimit);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, adClientId);
			ps.setInt(c++, priceListVersionId);
			ps.setInt(c++, productId);
			
			int result = ps.executeUpdate();
			ps.close();
			
			if (!conn.getAutoCommit())
				conn.commit();

			return result;

		} catch (SQLException se) {
			System.err.println("Error on product id: " + productId);
			se.printStackTrace();
			conn.rollback();
		} finally {
			conn.setAutoCommit(true);
		}
		return 0;
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

	public int getPriceListVersionId() {
		return priceListVersionId;
	}

	public void setPriceListVersionId(int priceListVersionId) {
		this.priceListVersionId = priceListVersionId;
	}

	public int getProductId() {
		return productId;
	}

	public void setProductId(int productId) {
		this.productId = productId;
	}

	public double getPriceStd() {
		return priceStd;
	}

	public void setPriceStd(double priceStd) {
		this.priceStd = priceStd;
	}

	public double getPriceList() {
		return priceList;
	}

	public void setPriceList(double priceList) {
		this.priceList = priceList;
	}

	public double getPriceLimit() {
		return priceLimit;
	}

	public void setPriceLimit(double priceLimit) {
		this.priceLimit = priceLimit;
	}

	
}
