package org.notima.businessobjects.adapter.tools.table;

import java.util.List;

import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;

public class TenantTable extends GenericTable {

	private String adapterName = "-";
	private List<BusinessPartner<Object>> bpList;
	private boolean withInfo;
	private TenantInformationFactory tif;
	private String defaultCountryCode;
	
	
	public TenantTable(List<BusinessPartner<Object>> bpl) {
		this(bpl, false, null, null);
	}

	/**
	 * @param bpl					The tenants to list.
	 * @param withInfo				If true, stored tenant information is shown.
	 * @param tif					Where tenant information is stored. Can be null.
	 * @param defaultCountryCode	Country code used for tenants without one.
	 */
	public TenantTable(List<BusinessPartner<Object>> bpl, boolean withInfo, TenantInformationFactory tif, String defaultCountryCode) {

		this.withInfo = withInfo;
		this.tif = tif;
		this.defaultCountryCode = defaultCountryCode;

		addColumn("Adapter");
		addColumn("Tax id");
		addColumn("Name");
		if (withInfo) {
			addColumn("Output dir");
			addColumn("Report dir");
		}
		
		if (bpl==null || bpl.size()==0) {
			setEmptyTableText("No tenants");
			return;
		}
		
		bpList = bpl;
		populateRows();
		
	}

	public void setAdapterName(String adapterName) {
		this.adapterName = adapterName;
		populateRows();
	}

	private void populateRows() {
		
		if (getRows()!=null)
			this.getRows().clear();

		if (bpList==null) return;
		
		for (BusinessPartner<Object> p : bpList) {
			if (withInfo) {
				TenantInformation ti = lookupTenantInformation(p);
				addRow().addContent(adapterName, p.getTaxId(), p.getName(),
						ti!=null && ti.getDefaultOutputDirectory()!=null ? ti.getDefaultOutputDirectory() : "-",
						ti!=null && ti.getReportDirectory()!=null ? ti.getReportDirectory() : "-");
			} else {
				addRow().addContent(adapterName, p.getTaxId(), p.getName());
			}
		}
		
	}

	private TenantInformation lookupTenantInformation(BusinessPartner<Object> p) {
		if (tif==null || p.getTaxId()==null) return null;
		String countryCode = p.getCountryCode()!=null && p.getCountryCode().trim().length()>0 
				? p.getCountryCode().trim() : defaultCountryCode;
		return tif.getTenantInformation(new TaxSubjectIdentifier(p.getTaxId(), countryCode));
	}
	
	
}
