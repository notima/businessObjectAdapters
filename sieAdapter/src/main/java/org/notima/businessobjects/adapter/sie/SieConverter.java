package org.notima.businessobjects.adapter.sie;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.util.LocalDateUtils;
import org.notima.sie.SIEFile;
import org.notima.sie.TransRec;
import org.notima.sie.VerRec;

import java.util.ArrayList;
import java.util.List;
import org.notima.sie.ObjRec;

public class SieConverter {

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
	
}
