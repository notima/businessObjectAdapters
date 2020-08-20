package org.notima.generic.adempiere;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;

import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.PriceListLine;
import org.notima.generic.businessobjects.Product;

public class AdPriceList {

	/**
	 * Loads pricelist
	 * 
	 * @param priceListId
	 * @param conn
	 * @return
	 * @throws SQLException
	 */
	public static PriceList loadPriceList(int priceListId, Connection conn) throws SQLException {
		
		PriceList pl = null;
		
		String sql = "select name, (select iso_code from c_currency where c_currency_id=pl.c_currency_id) as currency, "
						+ "priceprecision from m_pricelist pl where m_pricelist_id=?";

		PreparedStatement ps = conn.prepareStatement(sql);
		ps.setInt(1, priceListId);
		ResultSet rs = ps.executeQuery();
		int c;
		if (rs.next()) {
			c = 1;
			pl = new PriceList();
			pl.setName(rs.getString(c++));
			pl.setCurrency(rs.getString(c++));
			pl.setPricePrecision(rs.getInt(c++));
		}
		rs.close();
		ps.close();
		
		return pl;
	}
	
	/**
	 * Map contents as follows:
	 * 
	 * @param rs
	 * @return
	 */
	public PriceList toGenericPriceList(Map[] rs) {
		
		PriceList pl = new PriceList();
		Product pr;
		PriceListLine line;
		Map<String,Object> row;
		String brandname;
		
		for (int i=0; i<rs.length; i++) {
			row = rs[i];
			pr = new Product();
			pr.setKey(row.get("value").toString());
			pr.setName(row.get("name").toString());
			pr.setTaxKey(row.get("taxkey").toString());
			pr.setUnit(row.get("x12de355").toString());
			brandname = (String)row.get("brandname");
			pr.setBrand(brandname);
			line = new PriceListLine();
			line.setStdPrice(((BigDecimal)row.get("pricestd")).doubleValue());
			line.setProduct(pr);
			line.setQtyAvail(((BigDecimal)row.get("qtyavail")).doubleValue());
			pl.addPriceListLine(line);
		}
		
		return pl;
		
	}
	
}
