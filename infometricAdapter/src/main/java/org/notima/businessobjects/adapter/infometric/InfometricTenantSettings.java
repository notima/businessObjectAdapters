package org.notima.businessobjects.adapter.infometric;

import java.util.Map;
import java.util.TreeMap;

import org.notima.generic.businessobjects.BasicProductMapping;
import org.notima.generic.ifacebusinessobjects.ProductMapping;

public class InfometricTenantSettings {

	public static final String DEFAULT_PRODUCT = "ELM";
	
	private Map<String, BasicProductMapping>	productMapping = new TreeMap<String, BasicProductMapping>();
	
	public void addProductMapping(String sourceProductId, String destinationProductId, String description) {
		
		BasicProductMapping bpm = new BasicProductMapping();
		bpm.setSourceProductId(sourceProductId);
		bpm.setDestinationProductId(destinationProductId);
		bpm.setDestinationName(description);
		productMapping.put(sourceProductId, bpm);
		
	}
	
	public ProductMapping getDefaultProductMapping() {
		return productMapping.get(DEFAULT_PRODUCT);
	}
	
	public ProductMapping getProductMapping(String productId) {
		ProductMapping p = productMapping.get(productId);
		if (p==null) {
			return getDefaultProductMapping();
		} else {
			return p;
		}
	}
	
}
