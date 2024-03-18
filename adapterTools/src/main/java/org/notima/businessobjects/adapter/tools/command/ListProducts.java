package org.notima.businessobjects.adapter.tools.command;

import java.util.List;

import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.notima.businessobjects.adapter.tools.table.ProductTable;
import org.notima.generic.businessobjects.Product;
import org.notima.generic.ifacebusinessobjects.BusinessObjectFactory;

@Command(scope = "notima", name = "list-products", description = "Lists products / services for given adapter and tenant")
@Service
public class ListProducts extends AdapterCommand {
	
	@Override
	public Object onExecute() throws Exception {

		populateAdapters();
		listProducts();
		
		return null;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void listProducts() throws Exception {

		if (adaptersToList.size()>0) {
			ProductTable tt = null;
			
			for (BusinessObjectFactory bf : adaptersToList) {
				
				bf.setTenant(orgNo, countryCode);
				
				List<Product<?>> bpl = 
						bf.lookupProductByName(null);
				if (bpl!=null) {
					tt = new ProductTable(bpl);
				} else {
					tt = new ProductTable(null);
				}

				tt.getShellTable().print(sess.getConsole());
				
			}
				
		}
		
	}
	
}
