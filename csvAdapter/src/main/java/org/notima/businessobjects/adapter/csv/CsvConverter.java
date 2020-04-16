package org.notima.businessobjects.adapter.csv;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.notima.generic.businessobjects.BasicBusinessObjectConverter;
import org.notima.generic.businessobjects.Order;

public class CsvConverter extends BasicBusinessObjectConverter<Object[], Object[]>{

	public static DateFormat dfmt = new SimpleDateFormat("yy-MM-dd");
	
	public static String[] headers = new String[] {
		"orderNo",
		"orderDate",
		"customerName",
		"grandTotal",
		"vatAmount",
		"taxBase",
		"paymentrule"
	};
	
	@Override
	public Object[] toNativeOrder(Order<?> src) throws Exception {
		
		if (src.getNativeOrder()!=null) {
			if (src.getNativeOrder() instanceof Object[])
				return (Object[])src.getNativeOrder();
		} 
		
		Object[] dst = new Object[headers.length];
		
		int c = 0;
		dst[c++] = src.getOrderKey();
		dst[c++] = src.getDateOrdered() instanceof Date ? dfmt.format(src.getDateOrdered()) : src.getDateOrdered();
		dst[c++] = src.getBusinessPartner()!=null ? src.getBusinessPartner().getName() : "";
		dst[c++] = src.getGrandTotal();
		dst[c++] = src.getVatTotal();
		dst[c++] = src.getGrandTotal() - src.getVatTotal();
		dst[c++] = src.getPaymentRule();
		
		return dst;
		
	}
	
}
