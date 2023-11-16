package org.notima.generic.adempiere.factory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Calendar;
import java.sql.Date;

import javax.sql.DataSource;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.Location;
import org.notima.generic.ifacebusinessobjects.MappingService;

public class IDempiereAptMapper implements MappingService {

	private DataSource ds;
	private int 	   clientId;
	private int		   orgId;
	
	public IDempiereAptMapper(DataSource ds, int clientId, int orgId) {
		
		this.ds = ds;
		this.clientId = clientId;
		this.orgId = orgId;
		
	}
	
	@Override
	public String mapSourceToTarget(String aptNo, String typeOfObject) {

		String query = "select bp.taxid as taxid from xs_contract_part cp " + 
				"join c_bpartner bp on (cp.c_bpartner_id=bp.c_bpartner_id) " + 
				"join xs_contract c on (cp.xs_contract_id=c.xs_contract_id) " + 
				"join xs_rentalobject ro on (c.xs_rentalobject_id=ro.xs_rentalobject_id) " + 
				"where ro.object_key=? and c.ad_client_id=? and c_ad_org_id=? and c.isactive='Y' and cp.isactive='Y' " + 
				"AND (c.end_date IS NULL OR c.end_date>=?) " + 
				"AND cp.contract_role = 'B' order by cp.share desc limit 1";
		
		
		Date untilDate = new java.sql.Date(Calendar.getInstance().getTimeInMillis());

		String result = null;
		
		try {
		
			Connection conn = ds.getConnection();
			PreparedStatement ps = conn.prepareStatement(query);
			ps.setString(1, aptNo);
			ps.setInt(2, clientId);
			ps.setInt(3, orgId);
			ps.setDate(4, untilDate);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				result = rs.getString(1);
			}
			rs.close();
			ps.close();
			conn.close();
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return result;
		
	}

	@Override
	public BusinessPartner<?> mapLocationToBusinessPartner(Location location, String aptNo) {
		// TODO Auto-generated method stub
		return null;
	}

}
