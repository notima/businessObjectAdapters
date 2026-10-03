package org.notima.businessobjects.adapter.sie;

import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.AccountingFileExporter;
import org.notima.sie.AccountRec;
import org.notima.sie.BalanceRec;
import org.notima.sie.ObjRec;
import org.notima.sie.RARRec;
import org.notima.sie.SIEFileType4;
import org.notima.sie.TransRec;
import org.notima.sie.VerRec;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Exports an {@link AccountingPeriodData} as a SIE type-4 file.
 *
 * <p>When {@code includeTransactions} is {@code true} the output contains the full
 * chart of accounts, opening/closing balances, and all vouchers ({@code #VER} records).
 * When {@code false} only accounts and balances are written — useful for transferring
 * a period's opening balances to another system.
 */
public class SieAccountingFileExporter implements AccountingFileExporter {

    @Override public String   getSystemName()        { return SieAdapter.SYSTEM_NAME; }
    @Override public String   getFormatName()        { return "SIE4"; }
    @Override public String   getFileDescription()   { return "SIE Files (*.si)"; }
    @Override public String[] getFileExtensions()    { return new String[]{"si"}; }
    @Override public String   getCountryCode()       { return "SE"; }
    @Override public boolean  supportsTransactions() { return true; }

    @Override
    public void exportFile(File dest, AccountingPeriodData period,
                           BusinessPartner<?> company, boolean includeTransactions) throws Exception {

        SIEFileType4 sieFile = new SIEFileType4(dest.getAbsolutePath());

        if (company != null) {
            if (company.getTaxId() != null) sieFile.setOrgNr(company.getTaxId());
            if (company.getName()  != null) sieFile.setFNamn(company.getName());
        }

        addRarRecord(sieFile, period);
        addChartOfAccounts(sieFile, period);

        if (includeTransactions) {
            addVouchers(sieFile, period);
        }

        sieFile.writeToFile();
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void addRarRecord(SIEFileType4 sieFile, AccountingPeriodData period) {
        LocalDate start = period.getPeriodStart();
        LocalDate end   = period.getPeriodEnd();
        if (start == null || end == null) return;
        RARRec rar = new RARRec();
        rar.setRarNo(0);
        rar.setStartDate(java.sql.Date.valueOf(start));
        rar.setEndDate(java.sql.Date.valueOf(end));
        sieFile.addRARRec(rar);
    }

    private void addChartOfAccounts(SIEFileType4 sieFile, AccountingPeriodData period) {
        List<AccountElement> coa = period.getChartOfAccounts();
        if (coa == null) return;
        for (AccountElement ae : coa) {
            if (ae.getAccountNo() == null) continue;
            String name = ae.getName() != null ? ae.getName() : "";
            sieFile.addAccountRecord(new AccountRec(ae.getAccountNo(), name));
            if (ae.getOpeningBalance() != 0.0) {
                sieFile.addBalanceRecord(new BalanceRec(ae.getAccountNo(), ae.getOpeningBalance()));
            }
            if (ae.getEndingBalance() != 0.0) {
                BalanceRec ub = new BalanceRec(ae.getAccountNo(), ae.getEndingBalance());
                ub.setInBalance(false);
                sieFile.addBalanceRecord(ub);
            }
        }
    }

    private void addVouchers(SIEFileType4 sieFile, AccountingPeriodData period) {
        List<AccountingVoucher> vouchers = period.getVouchers();
        if (vouchers == null) return;
        for (AccountingVoucher voucher : vouchers) {
            sieFile.addVerRecord(convertVoucher(voucher, sieFile));
        }
    }

    private VerRec convertVoucher(AccountingVoucher voucher, SIEFileType4 sieFile) {
        VerRec ver = new VerRec();
        ver.setSerie(voucher.getVoucherSeries());
        ver.setVerNr(voucher.getVoucherNo());
        ver.setVerText(voucher.getDescription());
        if (voucher.getAcctDate() != null) {
            ver.setVerDatum(java.sql.Date.valueOf(voucher.getAcctDate()));
        }
        List<AccountingVoucherLine> lines = voucher.getLines();
        if (lines != null) {
            for (AccountingVoucherLine line : lines) {
                BigDecimal balance = line.getBalance();
                if (balance == null || balance.compareTo(BigDecimal.ZERO) == 0) continue;
                TransRec tr = new TransRec();
                tr.setKontoNr(line.getAcctNo());
                tr.setBelopp(balance.doubleValue());
                tr.setTransText(line.getDescription());
                List<ObjRec> dims = buildDimensions(voucher, line, sieFile);
                if (!dims.isEmpty()) tr.setObjektLista(dims);
                ver.addTransRec(tr);
            }
        }
        return ver;
    }

    private List<ObjRec> buildDimensions(AccountingVoucher voucher,
                                          AccountingVoucherLine line,
                                          SIEFileType4 sieFile) {
        List<ObjRec> dims = new ArrayList<>();
        // Line-level takes precedence over voucher-level
        String cc   = line.getCostCenter()  != null ? line.getCostCenter()  : voucher.getCostCenter();
        String proj = line.getProjectCode() != null ? line.getProjectCode() : voucher.getProjectCode();
        if (cc != null && !cc.trim().isEmpty()) {
            ObjRec o = sieFile.addCostCenter(cc.trim(), null);
            if (o != null) dims.add(o);
        }
        if (proj != null && !proj.trim().isEmpty()) {
            ObjRec o = sieFile.addProject(proj.trim(), null);
            if (o != null) dims.add(o);
        }
        return dims;
    }
}
