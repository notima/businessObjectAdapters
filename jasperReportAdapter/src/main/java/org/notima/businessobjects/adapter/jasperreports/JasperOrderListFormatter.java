package org.notima.businessobjects.adapter.jasperreports;

import java.util.Properties;

import org.notima.businessobjects.adapter.tools.OrderListFormatter;
import org.notima.generic.businessobjects.OrderList;

public class JasperOrderListFormatter extends JasperBasePdfFormatter implements OrderListFormatter {
	
	public final static String JASPER_COMPANY_NAME = "JasperCompanyName";
	public final static String JASPER_TAX_ID = "JasperTaxId";
	
	/**
	 * Formats an order list
	 */
	@Override
	public String formatOrderList(OrderList orderList, String format, Properties props) throws Exception {

		if (orderList==null || orderList.getOrderList()==null) {
			throw new Exception("OrderList can't be null");
		}
		
		Object[] data = orderList.getOrderList().toArray();
		
		String jasperCompanyName = null;
		String jasperTaxId = null;
		
		if (props!=null) {
			jasperCompanyName = props.getProperty(JASPER_COMPANY_NAME);
			jasperTaxId = props.getProperty(JASPER_TAX_ID);
		}
		
		Properties jasperProps = new Properties();
		if (jasperCompanyName!=null) {
			jasperProps.setProperty(JASPER_COMPANY_NAME, jasperCompanyName);
		}
		if (jasperTaxId!=null) {
			jasperProps.setProperty(JASPER_TAX_ID, jasperTaxId);
		}

		JasperParameterCallback jpc = null;
		if (!jasperProps.isEmpty()) {
			jpc = new JasperParameterCallback() {
				@Override
				public Properties getExtraProperties() {
					return jasperProps;
				}
			};
		}

		if ("pdf".equalsIgnoreCase(format) || format==null) {
			return formatReportAsPdf(
					data, 
					null, 
					jpc, 
					props);
		} else {
			
			return null;
		}
		
	}

	
}
