package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Location;


public class AdempiereDunningEntry {
	private int entryId;
	private int partnerId;
	private int partnerLocationId;
	private int dunningLevelId;
	
	private String creditorName;
	private String creditorPhone;
	private String creditorEmail;
	private String creditorBG;

	private String creditorAdress1;
	private String creditorAdress2;
	private String creditorAdress3;
	private String creditorAdress4;
	private String creditorCity;
	private String creditorPostal;

	private ArrayList<Integer> invoiceIds = new ArrayList<Integer>();
	
	private String description;

	private String referenceNo;

	public static ArrayList<AdempiereDunningEntry> loadAllForDunningRun(int dunningRunId, Connection conn) throws Exception{
		ArrayList<AdempiereDunningEntry> result = new ArrayList<AdempiereDunningEntry>();
		
		PreparedStatement ps = conn.prepareStatement(
				"SELECT dre.c_dunningrunentry_id ,dre.c_bpartner_id, dre.c_bpartner_location_id, dre.c_dunninglevel_id, " + 
				"(SELECT name FROM ad_org o WHERE o.ad_org_id=dre.ad_org_id), oi.Phone, oi.email, oi.bp_bankgiro, " +
				"l.address1, l.address2, l.address3, l.address4, l.city, l.postal " +
				"FROM c_dunningrunentry dre, AD_orgInfo oi, C_Location l WHERE dre.c_dunningrun_id=? AND oi.ad_org_id=dre.ad_org_id AND l.C_Location_ID=oi.C_Location_ID"
		);
		ps.setInt(1,dunningRunId);
		ResultSet rs = ps.executeQuery();
		
		while(rs.next()){
			result.add(new AdempiereDunningEntry(rs));
		}
		rs.close();
		ps.close();		
		
		for(AdempiereDunningEntry entry : result){
			ps = conn.prepareStatement("SELECT c_invoice_id, description FROM c_dunningrunline WHERE c_dunningrunentry_id=?");
			ps.setInt(1,entry.entryId);
			rs = ps.executeQuery();
			while(rs.next()){
				if(rs.getInt(1) != 0){
					entry.invoiceIds.add(rs.getInt(1));
					if(rs.getString(2) != null && rs.getString(2) != ""){
						entry.description = rs.getString(2);
					}
				}
			}
			ps.close();
			rs.close();
		}
		return result;
	}
	
	

	public static DunningEntry convert(AdempiereDunningEntry entry, Connection conn) throws Exception{
		DunningEntry dunE = new DunningEntry();
		
		BusinessPartner debtor = AdempiereBusinessPartner.load(entry.getPartnerId(), conn);
		dunE.setDebtor(debtor);
		Location billAdress = AdempiereLocation.convert(AdempiereLocation.findById(conn, entry.getPartnerLocationId()));
		dunE.getDebtor().setAddressOfficial(billAdress);
		BusinessPartner creditor = new BusinessPartner();
		Location creLoc = new Location();
		creLoc.setAddress1(entry.getCreditorAdress1());
		creLoc.setAddress2(entry.getCreditorAdress2());
		creLoc.setAddress3(entry.getCreditorAdress3());
		creLoc.setAddress4(entry.getCreditorAdress4());
		creLoc.setCity(entry.getCreditorCity());
		creLoc.setEmail(entry.getCreditorEmail());
		creLoc.setPhone(entry.getCreditorPhone());
		creLoc.setPostal(entry.getCreditorPostal());
		
		creditor.setAddressOfficial(creLoc);
		creditor.setName(entry.getCreditorName());
		dunE.setCreditor(creditor);
		dunE.setBgNo(entry.creditorBG);
		dunE.setLetterNo("" + entry.getEntryId());
		
		boolean hasNote = false;
		PreparedStatement ps = conn.prepareStatement("SELECT note FROM c_dunninglevel WHERE c_dunninglevel_id=?");
		ps.setInt(1,entry.getDunningLevelId());
		ResultSet rs = ps.executeQuery();
		if(rs.next()){
			hasNote = true;
		}
		
		for(int i = 0 ; i < entry.getInvoiceIds().size(); i++){
			Invoice invoice = AdempiereInvoice.load(entry.getInvoiceIds().get(i), conn);
			
			if(entry.getDescription() != null && entry.getDescription() != ""){
				invoice.setPaymentTermKey(entry.getDescription());
			}
			else if(hasNote){
				invoice.setPaymentTermKey(rs.getString(1));
			}
			else{
				invoice.setPaymentTermKey("note missing");
			}
			
			dunE.addInvoice(invoice);
		}
		
		ps.close();
		rs.close();
		
		if(dunE.getInvoices().size() == 1){
			// TODO: Check what this is good for
			// dunE.setOcrNo(dunE.getInvoices().get(0).getOcr());
		}
		
		return dunE;
	}
	
	public AdempiereDunningEntry(){};
	
	public AdempiereDunningEntry(ResultSet rs) throws Exception {
		int c=1;
		entryId = rs.getInt(c++);
		partnerId = rs.getInt(c++);
		partnerLocationId = rs.getInt(c++);
		dunningLevelId = rs.getInt(c++);
		creditorName = rs.getString(c++);
		creditorPhone = rs.getString(c++);
		creditorEmail = rs.getString(c++);
		creditorBG = rs.getString(c++);
		creditorAdress1 = rs.getString(c++);
		creditorAdress2 = rs.getString(c++);
		creditorAdress3 = rs.getString(c++);
		creditorAdress4 = rs.getString(c++);
		creditorCity = rs.getString(c++);
		creditorPostal = rs.getString(c++);
	}
	

	public int getEntryId() {
		return entryId;
	}
	public void setEntryId(int entryId) {
		this.entryId = entryId;
	}
	public int getPartnerId() {
		return partnerId;
	}
	public void setPartnerId(int partnerId) {
		this.partnerId = partnerId;
	}
	public int getPartnerLocationId() {
		return partnerLocationId;
	}
	public void setPartnerLocationId(int partnerLocationId) {
		this.partnerLocationId = partnerLocationId;
	}
	public int getDunningLevelId() {
		return dunningLevelId;
	}
	public void setDunningLevelId(int dunningLevelId) {
		this.dunningLevelId = dunningLevelId;
	}
	public ArrayList<Integer> getInvoiceIds() {
		return invoiceIds;
	}
	public void setInvoiceIds(ArrayList<Integer> invoiceIds) {
		this.invoiceIds = invoiceIds;
	}
	public String getCreditorName() {
		return creditorName;
	}
	public void setCreditorName(String creditorName) {
		this.creditorName = creditorName;
	}
	public String getCreditorPhone() {
		return creditorPhone;
	}
	public void setCreditorPhone(String creditorPhone) {
		this.creditorPhone = creditorPhone;
	}
	public String getCreditorEmail() {
		return creditorEmail;
	}
	public void setCreditorEmail(String creditorEmail) {
		this.creditorEmail = creditorEmail;
	}
	public String getCreditorBG() {
		return creditorBG;
	}
	public void setCreditorBG(String creditorBG) {
		this.creditorBG = creditorBG;
	}
	public String getCreditorAdress1() {
		return creditorAdress1;
	}
	public void setCreditorAdress1(String creditorAdress1) {
		this.creditorAdress1 = creditorAdress1;
	}
	public String getCreditorAdress2() {
		return creditorAdress2;
	}
	public void setCreditorAdress2(String creditorAdress2) {
		this.creditorAdress2 = creditorAdress2;
	}
	public String getCreditorAdress3() {
		return creditorAdress3;
	}
	public void setCreditorAdress3(String creditorAdress3) {
		this.creditorAdress3 = creditorAdress3;
	}
	public String getCreditorAdress4() {
		return creditorAdress4;
	}
	public void setCreditorAdress4(String creditorAdress4) {
		this.creditorAdress4 = creditorAdress4;
	}
	public String getCreditorCity() {
		return creditorCity;
	}
	public void setCreditorCity(String creditorCity) {
		this.creditorCity = creditorCity;
	}
	public String getCreditorPostal() {
		return creditorPostal;
	}
	public void setCreditorPostal(String creditorPostal) {
		this.creditorPostal = creditorPostal;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getReferenceNo() {
		return referenceNo;
	}
	public void setReferenceNo(String referenceNo) {
		this.referenceNo = referenceNo;
	}
}
