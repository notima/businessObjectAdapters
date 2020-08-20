package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdempiereContact {
	
	private int		adClientId;
	private int		adOrgId;
	private int		userId;
	private int		partnerId;
	private String	user;
	private String	name;
	private String	email;
	private String	phone;
	
	private static String selectSql = 
			"select u.ad_client_id, u.ad_org_id, u.c_bpartner_id, u.name, u.email, u.phone, " +
			"u.value, u.ad_user_id " +
			"from " +
			"ad_user u ";
	
	private static String updateSql = 
			"update ad_user set ad_client_id=?, ad_org_id=?, c_bpartner_id=?, name=?, email=?, phone=?, " +
					"value=? where ad_user_id=?";
	
	private static String insertSql = 
			"insert into ad_user " + 
					"(ad_client_id, ad_org_id, createdby, updatedby, c_bpartner_id, name, email, phone, " +
					" value, " +
					"ad_user_id, ad_user_uu) " + 
					"values (?,?,0,0,?,?,?,?,?, nextval('ad_user_sq'), uuid_generate_v4())";
	
	public AdempiereContact(ResultSet rs) throws SQLException {
		int c = 1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		partnerId = rs.getInt(c++);
		name = rs.getString(c++);
		email = rs.getString(c++);
		phone = rs.getString(c++);
		user = rs.getString(c++);
		userId = rs.getInt(c++);
	}
	
	public AdempiereContact() {
	}

	
	public static AdempiereContact load(Connection conn, int adUserId) throws SQLException {
		PreparedStatement ps = conn.prepareStatement(selectSql + " where ad_user_id=?");
		ps.setInt(1, adUserId);
		
		AdempiereContact c = null;
		ResultSet rs = ps.executeQuery();
		if (rs.next()) {
			c = new AdempiereContact(rs);
		}
		rs.close();
		ps.close();
		return c;
		
	}
	
	public int persist(Connection conn) throws SQLException {
		
		// Start transaction
		conn.setAutoCommit(false);

		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select ad_user_id from ad_user where ad_client_id=? and value=?");
			ps.setInt(1, adClientId);
			ps.setString(2,  user);
			ResultSet rs = ps.executeQuery();
			int tmpUserId = 0;
			if (rs.next()) {
				tmpUserId = rs.getInt(1);
			}
			userId=tmpUserId;
			rs.close();
			ps.close();
			int c = 1;
			ps = conn.prepareStatement(tmpUserId>0 ? updateSql : insertSql);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, adClientId);
			ps.setInt(c++, partnerId);
			ps.setString(c++, name);
			ps.setString(c++, email);
			ps.setString(c++, phone);
			ps.setString(c++, user);
			if (userId>0) {
				ps.setInt(c++, userId);
			}
			int result = ps.executeUpdate();
			ps.close();

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
		
		if (user!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(user);
		}
		
		if (name!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(name);
		}
		
		return buf.toString();
		
	}
	
	public static List<AdempiereContact> findByName(Connection conn, int clientId, String name) throws SQLException {

		List<AdempiereContact> result = new ArrayList<AdempiereContact>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where (value=? or name ilike ?) and AD_Client_ID=?");
		ps.setString(1, name);
		ps.setString(2, "%"+name+"%");
		ps.setInt(3, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereContact(rs));
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

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public String getUser() {
		return user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}
	
	
}
