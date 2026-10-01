package org.notima.businessobjects.adapter.tools;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

import org.notima.generic.ifacebusinessobjects.InvoiceDeliveryMethod;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

public class InvoiceDeliveryMethodFactoryImpl implements InvoiceDeliveryMethodFactory {

	private BundleContext ctx;

	public void setBundleContext(BundleContext c) {
		ctx = c;
	}

	@Override
	public InvoiceDeliveryMethod getInvoiceDeliveryMethod(String type) {
		return getMethods().get(type);
	}

	@Override
	public Collection<String> getInvoiceDeliveryTypes() {
		return getMethods().keySet();
	}

	private Map<String, InvoiceDeliveryMethod> getMethods() {
		Map<String, InvoiceDeliveryMethod> methods = new TreeMap<String, InvoiceDeliveryMethod>();
		try {
			Collection<ServiceReference<InvoiceDeliveryMethod>> references = ctx.getServiceReferences(InvoiceDeliveryMethod.class, null);
			if (references!=null) {
				for (ServiceReference<InvoiceDeliveryMethod> sr : references) {
					InvoiceDeliveryMethod method = ctx.getService(sr);
					if (method!=null) {
						methods.put(method.getType(), method);
					}
				}
			}
		} catch (InvalidSyntaxException e) {
			e.printStackTrace();
		}
		return methods;
	}

}
