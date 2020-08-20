package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.notima.generic.businessobjects.Location;

public class AdempiereLocation {
	
	private int		adClientId;
	private int		adOrgId;
	private int		partnerLocationId;
	private int		partnerId;
	private String	name;
	private String	phone;
	private boolean	isBillTo;
	private boolean	isShipTo;
	private boolean	isPayFrom;
	private boolean	isRemitTo;
	private int		locationId;
	private String	address1;
	private String	address2;
	private String	address3;
	private String	address4;
	private	String	city;
	private String	postal;
	private String	countryCode;
	private boolean isActive;
	private String	customerAddressId;
	
	private static String selectSql = 
			"select bl.ad_client_id, bl.ad_org_id, bl.c_bpartner_id, bl.name, bl.phone, " +
			"bl.isbillto, bl.isshipto, bl.ispayfrom, bl.isremitto, " +
			"loc.c_location_id, loc.address1, loc.address2, loc.address3, loc.address4, " +
			"loc.city, loc.postal, (select countrycode from c_country where c_country_id=loc.c_country_id) as countrycode, " +
			"bl.isactive, bl.c_bpartner_location_id " +
			"from " +
			"c_bpartner_location bl " +
			"left join c_location loc on (bl.c_location_id=loc.c_location_id)";
	
	private static String updateSql = 
			"update c_bpartner_location set ad_client_id=?, ad_org_id=?, c_bpartner_id=?, name=?, phone=?, " +
					"isbillto=?, isshipto=?, ispayfrom=?, isremitto=?, " +
					"c_location_id=?, isactive=? " + 
					"where c_bpartner_location_id=?";
	
	private static String updateSqlLocation =
			"update c_location set ad_client_id=?, ad_org_id=?, " + 
			"address1=?, address2=?, address3=?, address4=?, " + 
			"city=?, postal=?, c_country_id=(select c_country_id from c_country where countrycode=?) " + 
			"where c_location_id=?";
	
	private static String selectLocationSql = 
			"select ad_client_id, ad_org_id, c_location_id, " + 
			"address1, address2, address3, address4, " + 
			"city, postal, (select countrycode from c_country where c_country_id=loc.c_country_id) as countrycode, " + 
			"isactive from c_location loc where c_location_id=?";
	
	private static String insertSql = 
			"insert into c_bpartner_location " + 
					"(ad_client_id, ad_org_id, createdby, updatedby, c_bpartner_id, name, phone, " +
					"isbillto, isshipto, ispayfrom, isremitto, " +
					"c_location_id, isactive, customeraddressid," + 
					"c_bpartner_location_id, c_bpartner_location_uu) " + 
					"values " + 
					"(?,?,0,0,?,?,?," + 
					"?,?,?,?, " +
					"?,?,?," +
					"nextval('c_bpartner_location_sq'), uuid_generate_v4())";
	
	private static String insertSqlLocation = 
			"insert into c_location " + 
					"(ad_client_id, ad_org_id, createdby, updatedby, " + 
					"address1, address2, address3, address4, " + 
					"city, postal, c_country_id, c_location_id, c_location_uu) " + 
					"values (?,?,0,0," + 
					"?,?,?,?," + 
					"?,?,(select c_country_id from c_country where countrycode=?), nextval('c_location_sq'),  uuid_generate_v4())";
	
	
	public AdempiereLocation(ResultSet rs) throws SQLException {
		int c = 1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		partnerId = rs.getInt(c++);
		name = rs.getString(c++);
		phone = rs.getString(c++);
		isBillTo = "Y".equalsIgnoreCase(rs.getString(c++));
		isShipTo = "Y".equalsIgnoreCase(rs.getString(c++));
		isPayFrom = "Y".equalsIgnoreCase(rs.getString(c++));
		isRemitTo = "Y".equalsIgnoreCase(rs.getString(c++));
		locationId = rs.getInt(c++);
		address1 = rs.getString(c++);
		address2 = rs.getString(c++);
		address3 = rs.getString(c++);
		address4 = rs.getString(c++);
		city = rs.getString(c++);
		postal = rs.getString(c++);
		countryCode = rs.getString(c++);
		isActive = "Y".equalsIgnoreCase(rs.getString(c++));
	}
	
	public AdempiereLocation() {
	}

	/**
	 * Create a location from the selectLocationSql
	 * 
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	public static AdempiereLocation createFromLocation(ResultSet rs) throws SQLException {
		int c = 1;
		AdempiereLocation r = new AdempiereLocation();
		r.adClientId = rs.getInt(c++);
		r.adOrgId = rs.getInt(c++);
		r.locationId = rs.getInt(c++);
		r.address1 = rs.getString(c++);
		r.address2 = rs.getString(c++);
		r.address3 = rs.getString(c++);
		r.address4 = rs.getString(c++);
		r.city = rs.getString(c++);
		r.postal = rs.getString(c++);
		r.countryCode = rs.getString(c++);
		r.isActive = "Y".equalsIgnoreCase(rs.getString(c++));
		return r;
	}
	
	
	/**
	 * Use customer address id as key
	 * 
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public int persist(Connection conn) throws SQLException {
		
		// Start transaction
		conn.setAutoCommit(false);

		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select c_bpartner_location_id, c_location_id from c_bpartner_location where ad_client_id=? and customeraddressid=?");
			ps.setInt(1, adClientId);
			ps.setString(2,  customerAddressId);
			ResultSet rs = ps.executeQuery();
			int tmpPartnerLocationId = 0;
			int tmpLocationId = 0;
			if (rs.next()) {
				tmpPartnerLocationId = rs.getInt(1);
				tmpLocationId = rs.getInt(2);
			}
			partnerLocationId=tmpPartnerLocationId;
			if (locationId==0 && tmpLocationId>0)
				locationId = tmpLocationId;
			rs.close();
			ps.close();
			
			// Update / insert location first
			int c = 1;
			ps = conn.prepareStatement(tmpLocationId==0 ? insertSqlLocation : updateSqlLocation);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, adClientId);
			ps.setString(c++, address1);
			ps.setString(c++, address2);
			ps.setString(c++, address3);
			ps.setString(c++, address4);
			ps.setString(c++, city);
			ps.setString(c++, postal);
			ps.setString(c++, countryCode);
			if (locationId>0)
				ps.setInt(c++, locationId);
			ps.executeUpdate();
			ps.close();
			
			// Get last id
			if (tmpLocationId==0) {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery("select currval('c_location_sq')");
				if (rs.next())
					locationId = rs.getInt(1);
				rs.close();
				stmt.close();
			}
			
			c = 1;
			ps = conn.prepareStatement(tmpPartnerLocationId>0 ? updateSql : insertSql);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, adClientId);
			ps.setInt(c++, partnerId);
			ps.setString(c++, name);
			ps.setString(c++, phone);
			ps.setString(c++, (isBillTo ? "Y" : "N"));
			ps.setString(c++, (isShipTo ? "Y" : "N"));
			ps.setString(c++, (isPayFrom ? "Y" : "N"));
			ps.setString(c++, (isRemitTo ? "Y" : "N"));
			ps.setInt(c++, locationId);
			ps.setString(c++,  (isActive ? "Y" : "N"));
			ps.setString(c++, customerAddressId);
			if (partnerLocationId>0) {
				ps.setInt(c++, partnerLocationId);
			}
			int result = ps.executeUpdate();
			ps.close();

			// Get last id
			if (tmpPartnerLocationId==0) {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery("select currval('c_bpartner_location_sq')");
				if (rs.next())
					partnerLocationId = rs.getInt(1);
				rs.close();
				stmt.close();
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
		
		if (name!=null) {
			if (buf.length()>0) buf.append(" ");
			buf.append(name);
		}
		
		return buf.toString();
		
	}

	public static AdempiereLocation findById(Connection conn, int partnerLocationId) throws SQLException {

		AdempiereLocation result = null;
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where c_bpartner_location_id=?");
		ps.setInt(1, partnerLocationId);
		ResultSet rs = ps.executeQuery();
		if(rs.next()) {
			result = new AdempiereLocation(rs);
		}
		rs.close();
		ps.close();
		
		return result;
	}
	
	public static AdempiereLocation findByCLocation(Connection conn, int cLocationId) throws SQLException {
		
		AdempiereLocation result = null;
		PreparedStatement ps = conn.prepareStatement(selectLocationSql);
		ps.setInt(1, cLocationId);
		
		ResultSet rs = ps.executeQuery();
		
		if (rs.next()) {
			result = AdempiereLocation.createFromLocation(rs);
		}
		rs.close();
		ps.close();
		
		return result;
		
	}
	
	public static Location convert(AdempiereLocation src) {
		
			if (src==null) return null;
			Location dst = new Location();
			
			dst.setName(src.getName());
			dst.setAddress1(src.getAddress1());
			dst.setAddress2(src.getAddress2());
			dst.setAddress3(src.getAddress3());
			dst.setAddress4(src.getAddress4());
			dst.setCity(src.getCity());
			dst.setCountryCode(src.getCountryCode());
			dst.setPostal(src.getPostal());
			dst.setPhone(src.getPhone());
			if (src.getCustomerAddressId()!=null && src.getCustomerAddressId().trim().length()>0) {
				try {
					dst.setLocationId(Integer.parseInt(src.getCustomerAddressId()));
				} catch (Exception e) {}
			} 
			
			return dst;
	}
	
	public static List<AdempiereLocation> findByName(Connection conn, int clientId, String name) throws SQLException {

		List<AdempiereLocation> result = new ArrayList<AdempiereLocation>();
		
		PreparedStatement ps = conn.prepareStatement(selectSql + " where (value=? or name ilike ?) and AD_Client_ID=?");
		ps.setString(1, name);
		ps.setString(2, "%"+name+"%");
		ps.setInt(3, clientId);
		ResultSet rs = ps.executeQuery();
		while(rs.next()) {
			result.add(new AdempiereLocation(rs));
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

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public int getPartnerLocationId() {
		return partnerLocationId;
	}

	public void setPartnerLocationId(int partnerLocationId) {
		this.partnerLocationId = partnerLocationId;
	}

	public boolean isBillTo() {
		return isBillTo;
	}

	public void setBillTo(boolean isBillTo) {
		this.isBillTo = isBillTo;
	}

	public boolean isShipTo() {
		return isShipTo;
	}

	public void setShipTo(boolean isShipTo) {
		this.isShipTo = isShipTo;
	}

	public boolean isPayFrom() {
		return isPayFrom;
	}

	public void setPayFrom(boolean isPayFrom) {
		this.isPayFrom = isPayFrom;
	}

	public boolean isRemitTo() {
		return isRemitTo;
	}

	public void setRemitTo(boolean isRemitTo) {
		this.isRemitTo = isRemitTo;
	}

	public int getLocationId() {
		return locationId;
	}

	public void setLocationId(int locationId) {
		this.locationId = locationId;
	}

	public String getAddress1() {
		return address1;
	}

	public void setAddress1(String address1) {
		this.address1 = address1;
	}

	public String getAddress2() {
		return address2;
	}

	public void setAddress2(String address2) {
		this.address2 = address2;
	}

	public String getAddress3() {
		return address3;
	}

	public void setAddress3(String address3) {
		this.address3 = address3;
	}

	public String getAddress4() {
		return address4;
	}

	public void setAddress4(String address4) {
		this.address4 = address4;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getPostal() {
		return postal;
	}

	public void setPostal(String postal) {
		this.postal = postal;
	}

	public String getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(String countryCode) {
		this.countryCode = countryCode;
	}

	public boolean isActive() {
		return isActive;
	}

	public void setActive(boolean isActive) {
		this.isActive = isActive;
	}

	public String getCustomerAddressId() {
		return customerAddressId;
	}

	public void setCustomerAddressId(String customerAddressId) {
		this.customerAddressId = customerAddressId;
	}
	
	
}
