package org.notima.businessobjects.adapter.sie;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.BasicBusinessObjectFactory;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.BusinessPartnerList;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;
import org.notima.generic.businessobjects.Order;
import org.notima.generic.businessobjects.PaymentTerm;
import org.notima.generic.businessobjects.PriceList;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.businessobjects.ProductCategory;
import org.notima.generic.businessobjects.Tax;
import org.notima.generic.ifacebusinessobjects.FactoringReservation;
import org.notima.sie.SIEFileType4;
import org.notima.sie.SIEUtil;
import org.notima.sie.VerRec;

public class SieAdapter extends BasicBusinessObjectFactory<
Object,
Object,
Object,
Object,
Object,
Object> {

	public static final String SYSTEM_NAME = "SIE";
	public static final String PROP_SIE_FILE_PATH = "SieFilePath";
	public static final String PROP_SIE_FILE_PREFIX = "SieFilePrefix";
	
	@Override
	public String getSystemName() {
		return SYSTEM_NAME;
	}
	
	@Override
	public List<AccountingVoucher> writeVouchers(List<AccountingVoucher> vouchers) throws Exception {
		String destinationPath = this.getSetting(PROP_SIE_FILE_PATH);
		String filePrefix = this.getSetting(PROP_SIE_FILE_PREFIX);
		if (filePrefix==null) {
			filePrefix = "";
		}
		if (destinationPath==null) {
			destinationPath = System.getProperty("user.home") + File.separator + filePrefix + "SIE4.si";
		}
		if (!destinationPath.toUpperCase().endsWith(".SI")) {
			if (!destinationPath.endsWith(File.separator))
				destinationPath += File.separator;
			destinationPath += filePrefix + "SIE4.si";
		}
		
		SIEFileType4 sie4file = new SIEFileType4(destinationPath); 
		
		BusinessPartner<?> tenant = getCurrentTenant();
		
		if (tenant!=null) {
			if (tenant.getTaxId()!=null)
				sie4file.setOrgNr(tenant.getTaxId());
			if (tenant.getName()!=null)
				sie4file.setFNamn(tenant.getName());
		}
		
		sie4file.setProgram(SIEUtil.SIEFileLibString);
		
		VerRec vr;
		
		SieConverter sieConverter = new SieConverter(sie4file);
		
		for (AccountingVoucher v : vouchers) {
			vr = sieConverter.convert(v);
			sie4file.addVerRecord(vr);
		}
		sie4file.writeToFile();
		
		return vouchers;
	}

	@Override
	public BusinessPartnerList<Object> listTenants() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<Object> lookupBusinessPartner(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupAllBusinessPartners() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<BusinessPartner<?>> lookupBusinessPartners(int maxCount, boolean customers, boolean suppliers)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public BusinessPartner<Object> lookupThisCompanyInformation() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public DunningRun<?, ?> lookupDunningRun(String key, Date dueDateUntil) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object getClient() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupNativeInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persistNativeInvoice(Object invoice) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object lookupNativeOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persistNativeOrder(Object order) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Invoice<Object> lookupInvoice(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Order<Object> lookupOrder(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupProduct(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupProductByEan(String ean) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Product<Object>> lookupProductByName(String name) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PriceList lookupPriceForProduct(String productKey, String currency, Boolean salesPriceList)
			throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<ProductCategory> lookupProductCategory(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Object, Object> lookupList(String listName, boolean customer) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Product<Object> lookupRoundingProduct() throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tax lookupTax(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PaymentTerm lookupPaymentTerm(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public FactoringReservation lookupFactoringReservation(String key) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForOrder(String orderKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<FactoringReservation> lookupFactoringReservationForInvoice(String invoiceKey) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object persist(Object o) throws Exception {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isConnected() throws Exception {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void destroy() throws Exception {
		// TODO Auto-generated method stub
		
	}


}
