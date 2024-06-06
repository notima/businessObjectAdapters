package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannelFactory;
import org.notima.generic.ifacebusinessobjects.PaymentBatchProcessor;
import org.notima.generic.ifacebusinessobjects.PaymentFactory;
import org.notima.generic.ifacebusinessobjects.TaxRateProvider;

@Command(scope = "notima", name = "list-adapters", description = "Lists registered adapters")
@Service
public class ListAdapters implements Action {

	@Reference
	private Session sess;
	
	@SuppressWarnings("rawtypes")
	@Reference
	private List<BusinessObjectFactory> bofs;
	
	@Reference
	private List<PaymentFactory> pfs;
	
	@Reference
	private List<PaymentBatchProcessor> pbps;
	
	@Reference
	private List<TaxRateProvider> trps;
	
	@Reference
	private List<PaymentBatchChannelFactory> pbcs;
	
	@Override
	public Object execute() throws Exception {
	
		listBusinessObjectAdapters();

		listPaymentFactories();

		listPaymentBatchProcessors();
		
		listTaxRateProviders();
		
		listPaymentBatchChannelFactories();
		
		return null;
	}

	@SuppressWarnings("rawtypes")
	private void listBusinessObjectAdapters() {
		
		if (bofs==null) {
			System.out.println("No adapters registered");
		} else {
			System.out.println("Business Object Adapters");
			for (BusinessObjectFactory bf : bofs) {

				sess.getConsole().println(bf.getSystemName());
				
			}
			System.out.println(bofs.size() + " adapters registered");
		}
		
	}
	
	private void listPaymentFactories() {

		if (pfs==null) {
			System.out.println("No payment factories registered");
		} else {
			System.out.println("Payment Factories");
			for (PaymentFactory pf : pfs) {

				sess.getConsole().println(pf.getSystemName());
				
			}
			System.out.println(pfs.size() + " payment factories registered");
		}
		
	}
	
	private void listPaymentBatchProcessors() {

		if (pbps==null) {
			System.out.println("No payment batch processors registered");
		} else {
			System.out.println("Payment batch processors");
			for (PaymentBatchProcessor pbp : pbps) {

				sess.getConsole().println(pbp.getSystemName());
				
			}
			System.out.println(pbps.size() + " payment batch processors registered");
		}
		
	}
	
	
	private void listTaxRateProviders() {
		
		if (trps==null) {
			System.out.println("No tax rate providers registered");
		} else {
			System.out.println("Tax rate providers");
			for (TaxRateProvider trp : trps) {
				
				sess.getConsole().println(trp.getSystemName());
				
			}
		}
		
	}
	
	
	private void listPaymentBatchChannelFactories() {
		
		if (pbcs==null) {
			System.out.println("No Payment Batch Channel factories registered");
		} else {
			System.out.println("Payment Batch Channel factories");
			for (PaymentBatchChannelFactory pbcf : pbcs) {
				
				sess.getConsole().println(pbcf.getSystemName());
				
			}
		}

		
	}
	
}
