package org.notima.businessobjects.adapter.sie;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.AccountingVoucherConverter;
import org.notima.util.LocalDateUtils;
import org.notima.sie.SIEFile;
import org.notima.sie.TransRec;
import org.notima.sie.VerRec;

import java.util.ArrayList;
import java.util.List;
import org.notima.sie.ObjRec;

public class SieConverter implements AccountingVoucherConverter<VerRec> {

	private SIEFile sieFile;
	
	public SieConverter(SIEFile sieFile) {
		this.sieFile = sieFile;
	}
	
	public List<ObjRec> getDimensions(AccountingVoucher av) {
		
		List<ObjRec> result = new ArrayList<ObjRec>();
		ObjRec project;
		ObjRec costCenter;
		if (av.getProjectCode()!=null && av.getProjectCode().trim().length()>0) {
			project = sieFile.addProject(av.getProjectCode().trim(), null);
			result.add(project);
		}
		if (av.getCostCenter()!=null && av.getCostCenter().trim().length()>0) {
			costCenter = sieFile.addCostCenter(av.getCostCenter(), null);
			result.add(costCenter);
		}
		return result;
		
	}
	
	/**
	 * Convert an accounting voucher to a verrec.
	 * 
	 * @param src
	 * @return
	 */
	public VerRec convert(AccountingVoucher src) {
		
		VerRec dst = new VerRec();
		dst.setSerie(src.getVoucherSeries());
		dst.setVerDatum(LocalDateUtils.asDate(src.getAcctDate()));
		dst.setVerText(src.getDescription());
		
		if (src.getLines()==null) return dst;
		
		TransRec tr = null;
		for (AccountingVoucherLine al : src.getLines()) {
			if (Math.round(al.getBalance().doubleValue()*100)==0)
				continue;
			tr = new TransRec();
			tr.setBelopp(al.getBalance().doubleValue());
			tr.setKontoNr(al.getAcctNo());
			tr.setTransText(al.getDescription());
			tr.setObjektLista(getDimensions(src));
			dst.addTransRec(tr);
		}
		
		return dst;
	}

	@Override
	public String getSystemName() {
		return SieAdapter.SYSTEM_NAME;
	}

	@Override
	public void readSource(VerRec src) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void setTenant(BusinessPartner tenant) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public BusinessPartner getTenant() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<AccountingVoucher> getAccountingVouchers() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
