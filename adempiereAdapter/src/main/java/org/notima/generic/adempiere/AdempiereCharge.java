package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AdempiereCharge {

	private int		adClientId;
	private int		adOrgId;
	private int		chargeId;
	private String  name;
	private String	description;
	
	private static String selectSql = "select ad_client_id, ad_org_id, c_charge_id, name, description";
	
	public AdempiereCharge() {}
	
	public AdempiereCharge(ResultSet rs) throws SQLException {
	
		int c = 1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		chargeId = rs.getInt(c++);
		name = rs.getString(c++);
		description = rs.getString(c++);
		
	}
	
	public static AdempiereCharge loadCharge(int chargeId, String lang, Connection conn) throws SQLException {
		
		String sql = selectSql + " from c_charge_trl where c_charge_id=? and ad_language=?";
		
		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setInt(1, chargeId);
		ps.setString(2, lang);
		
		AdempiereCharge result = null;
		
		ResultSet rs = ps.executeQuery();
		
		if (rs.next()) {
			result = new AdempiereCharge(rs);
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

	public int getChargeId() {
		return chargeId;
	}

	public void setChargeId(int chargeId) {
		this.chargeId = chargeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
	
	
	
}
