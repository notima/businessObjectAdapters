package org.notima.generic.adempiere;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class AdempiereFactoringReservation {
	
	private int		adClientId;
	private int		adOrgId;
	private int		factoringReservationId;
	private int		orderId;
	private double	amount;
	private boolean	cancelled;
	private String	currency;
	private int		factoringOptionId;
	private int		paymentTermId;
	private int		supplierAccountId;
	private String	supplierReference;
	
	private static String selectSql = 
			"select u.ad_client_id, u.ad_org_id, u.c_order_id, u.amount, u.cancelled, select(iso_code from c_currency where c_currency_id=u.c_currency_id), " +
			"xc_factoring_option_id, c_paymentterm_id, xc_sveaekonomi_accounts_id, supplier_reference, " +
			"xc_factoring_reservation_id " +
			"from " +
			"xc_factoring_reservation u ";
	
	private static String updateSql = 
			"update xc_factoring_reservation set ad_client_id=?, ad_org_id=?, c_order_id=?, amount=?, cancelled=?, c_currency_id=(select c_currency_id from c_currency where iso_code=?), " +
					"xc_factoring_option_id=?, c_paymentterm_id=?, xc_sveaekonomi_accounts_id=?, supplier_reference=? " +
					"where xc_factoring_reservation_id=?";
	
	private static String insertSql = 
			"insert into xc_factoring_reservation " + 
					"(ad_client_id, ad_org_id, createdby, updatedby, c_order_id, amount, cancelled, c_currency_id, " +
					"xc_factoring_option_id, c_paymentterm_id, xc_sveaekonomi_accounts_id, supplier_reference, " +
					"xc_factoring_reservation_id, xc_factoring_reservation_uu) " + 
					"values (?,?,0,0,?,?,?,(select c_currency_id from c_currency where iso_code=?), " +
					"?,?,?,?," +
					"nextval('xc_factoring_reservation_sq'), uuid_generate_v4())";
	
	public AdempiereFactoringReservation(ResultSet rs) throws SQLException {
		int c = 1;
		adClientId = rs.getInt(c++);
		adOrgId = rs.getInt(c++);
		orderId = rs.getInt(c++);
		
		factoringReservationId = rs.getInt(c++);
	}
	
	public AdempiereFactoringReservation() {
	}

	public int persist(Connection conn) throws SQLException {
		
		// Start transaction
		conn.setAutoCommit(false);

		try {
			// Check if exists
			PreparedStatement ps = conn.prepareStatement(
					"select xc_factoring_reservation_id from xc_factoring_reservation where ad_client_id=? and supplier_reference=?");
			ps.setInt(1, adClientId);
			ps.setString(2,  supplierReference);
			ResultSet rs = ps.executeQuery();
			int tmpId = 0;
			if (rs.next()) {
				tmpId = rs.getInt(1);
			}
			factoringReservationId=tmpId;
			rs.close();
			ps.close();
			int c = 1;
			ps = conn.prepareStatement(tmpId>0 ? updateSql : insertSql);
			ps.setInt(c++, adOrgId);
			ps.setInt(c++, adClientId);
			ps.setInt(c++, orderId);
			ps.setDouble(c++,  amount);
			ps.setString(c++, cancelled ? "Y" : "N");
			ps.setString(c++, currency);
			ps.setInt(c++, factoringOptionId);
			ps.setInt(c++, paymentTermId);
			ps.setInt(c++,  supplierAccountId);
			ps.setString(c++, supplierReference);
			
			if (factoringReservationId>0) {
				ps.setInt(c++, factoringReservationId);
			}
			int result = ps.executeUpdate();
			ps.close();

			// Get last id
			if (tmpId==0) {
				Statement stmt = conn.createStatement();
				rs = stmt.executeQuery("select currval('xc_factoring_reservation_sq')");
				if (rs.next())
					factoringReservationId = rs.getInt(1);
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

	public int getFactoringReservationId() {
		return factoringReservationId;
	}

	public void setFactoringReservationId(int factoringReservationId) {
		this.factoringReservationId = factoringReservationId;
	}

	public int getOrderId() {
		return orderId;
	}

	public void setOrderId(int orderId) {
		this.orderId = orderId;
	}

	public double getAmount() {
		return amount;
	}

	public void setAmount(double amount) {
		this.amount = amount;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public int getFactoringOptionId() {
		return factoringOptionId;
	}

	public void setFactoringOptionId(int factoringOptionId) {
		this.factoringOptionId = factoringOptionId;
	}

	public int getPaymentTermId() {
		return paymentTermId;
	}

	public void setPaymentTermId(int paymentTermId) {
		this.paymentTermId = paymentTermId;
	}

	public int getSupplierAccountId() {
		return supplierAccountId;
	}

	public void setSupplierAccountId(int supplierAccountId) {
		this.supplierAccountId = supplierAccountId;
	}

	public String getSupplierReference() {
		return supplierReference;
	}

	public void setSupplierReference(String supplierReference) {
		this.supplierReference = supplierReference;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String toString() {
		StringBuffer str = new StringBuffer();
		str.append("Amount => " + amount);
		str.append("\nCurrency => " + currency);
		str.append("\nSupplier reference => " + supplierReference);
		str.append("\nOrder ID => " + orderId);
		str.append("\nPaymentTerm ID => " + paymentTermId);
		str.append("\nFactoringReservation ID => " + factoringReservationId);
		str.append("\nSupplier Account ID => " + supplierAccountId);
		return str.toString();
	}
	
}
