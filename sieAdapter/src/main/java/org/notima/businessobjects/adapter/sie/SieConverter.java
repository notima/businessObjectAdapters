package org.notima.businessobjects.adapter.sie;

import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.util.LocalDateUtils;
import org.notima.sie.TransRec;
import org.notima.sie.VerRec;

public class SieConverter {

	/**
	 * Convert an accounting voucher to a verrec.
	 * 
	 * @param src
	 * @return
	 */
	public static VerRec convert(AccountingVoucher src) {
		
		VerRec dst = new VerRec();
		dst.setSerie(src.getVoucherSeries());
		dst.setVerDatum(LocalDateUtils.asDate(src.getAcctDate()));
		dst.setVerText(src.getDescription());
		
		TransRec tr = null;
		for (AccountingVoucherLine al : src.getLines()) {
			tr = new TransRec();
			tr.setBelopp(al.getBalance().doubleValue());
			tr.setKontoNr(al.getAcctNo());
			tr.setTransText(al.getDescription());
			dst.addTransRec(tr);
		}
		
		return dst;
	}
	
}
