package org.notima.businessobjects.adapter.fortnox;

import java.util.Date;
import java.util.Map;

import org.notima.api.fortnox.FortnoxClient3;
import org.notima.api.fortnox.entities3.CompanySetting;
import org.notima.api.fortnox.entities3.Customer;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.DunningEntry;
import org.notima.generic.businessobjects.DunningRun;
import org.notima.generic.businessobjects.Invoice;

public class FortnoxDunningRunner {

	private DunningRun<Customer,org.notima.api.fortnox.entities3.Invoice> dunningRun = new DunningRun<Customer,org.notima.api.fortnox.entities3.Invoice>();
	
	private boolean excludeNegativeOpenAmount;
	
	private Map<Object,Object> overdueList;
	
	private Invoice<org.notima.api.fortnox.entities3.Invoice> invoice;
	private DunningEntry<org.notima.api.fortnox.entities3.Customer, org.notima.api.fortnox.entities3.Invoice> dunningEntry;
	
	private FortnoxAdapter fortnoxAdapter;
	private FortnoxClient3 fortnoxClient;	
	
	private CompanySetting cs;
	
	private Date	   untilDueDate;
	
	public FortnoxDunningRunner(FortnoxAdapter fa, String key) throws Exception {

		excludeNegativeOpenAmount = key!=null && "excludeNegativeOpenAmount".equalsIgnoreCase(key);
		fortnoxAdapter = fa;
		fortnoxClient = fortnoxAdapter.getClient();
		cs = fortnoxClient.getCompanySetting();
		overdueList = fa.lookupList(FortnoxAdapter.LIST_UNPAIDOVERDUE);

	}
	
	public DunningRun<Customer, org.notima.api.fortnox.entities3.Invoice> getDunningRunResult() {
		return dunningRun;
	}
	
	public Date getUntilDueDate() {
		return untilDueDate;
	}

	public void setUntilDueDate(Date untilDueDate) {
		this.untilDueDate = untilDueDate;
	}

	@SuppressWarnings("unchecked")
	public void runDunningRun() throws Exception {
		
		for (Object o : overdueList.keySet()) {
			invoice = fortnoxAdapter.lookupInvoice(o.toString());
			
			if (!includeInvoice(invoice)) 
				continue;
			
			selectDunningEntryFor((BusinessPartner<Customer>) invoice.getBusinessPartner());
			invoice.setDescriptionKey(invoice.getLines().get(0).getName());
			dunningEntry.addInvoice(invoice);
			
			invoice.setNativeInvoice(null);
		}
		
	}
	
	
	private boolean includeInvoice(Invoice<org.notima.api.fortnox.entities3.Invoice> inv) {

		if (excludeNegativeOpenAmount && invoice.getOpenAmt()<0)
			return false;
		
		if (untilDueDate==null) return true;
		
		if (inv.getDueDate().after(untilDueDate))
			return false;
		
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	private void selectDunningEntryFor(BusinessPartner<Customer> bp) {
		
		if (bp==null) 
			dunningEntry = null;
		
		DunningEntry<Customer, org.notima.api.fortnox.entities3.Invoice> entry = 
				(DunningEntry<Customer, org.notima.api.fortnox.entities3.Invoice>) dunningRun.getFirstEntryForDebtor(bp);
	
		if (entry==null) {
			// Create new
			dunningEntry = new DunningEntry<Customer, org.notima.api.fortnox.entities3.Invoice>();
			dunningEntry.setCreditor(fortnoxAdapter.convert(cs));
			dunningEntry.setBgNo(fortnoxAdapter.getBgNo(cs));
			dunningEntry.setDebtor((BusinessPartner<Customer>) invoice.getBusinessPartner());
			dunningEntry.getDebtor().setAddressOfficial(invoice.getBillLocation());
			dunningEntry.setOcrNo(invoice.getOcr());
			dunningEntry.setLetterNo(invoice.getDocumentKey());
			dunningRun.addDunningEntry(dunningEntry);
			
		} else {
			dunningEntry = entry;
		}
		
		
	}
	
	
}
