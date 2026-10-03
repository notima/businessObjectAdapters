package org.notima.businessobjects.adapter.sie;

import org.notima.generic.businessobjects.AccountElement;
import org.notima.generic.businessobjects.AccountingFileData;
import org.notima.generic.businessobjects.AccountingPeriodData;
import org.notima.generic.businessobjects.AccountingVoucher;
import org.notima.generic.businessobjects.AccountingVoucherLine;
import org.notima.generic.businessobjects.BusinessPartner;
import org.notima.generic.ifacebusinessobjects.AccountingFileImporter;
import org.notima.sie.AccountRec;
import org.notima.sie.BalanceRec;
import org.notima.sie.DimRec;
import org.notima.sie.ObjRec;
import org.notima.sie.RARRec;
import org.notima.sie.SIEFileType4;
import org.notima.sie.TransRec;
import org.notima.sie.VerRec;
import org.notima.util.LocalDateUtils;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Imports SIE type-4 files (*.si, *.se, *.sie).
 *
 * <p>Conversion logic:
 * <ol>
 *   <li>Company info ({@code #FNAMN}, {@code #ORGNR}) → {@link BusinessPartner}</li>
 *   <li>Fiscal years ({@code #RAR}) → one {@link AccountingPeriodData} each</li>
 *   <li>{@code #KONTO} records → {@link AccountElement} list copied to every period</li>
 *   <li>{@code #IB}/{@code #UB} records → {@code openingBalance}/{@code endingBalance}
 *       on the matching period's account elements</li>
 *   <li>Verifications ({@code #VER}) → {@link AccountingVoucher} assigned to the matching period</li>
 *   <li>Transactions ({@code #TRANS}) → {@link AccountingVoucherLine}; account names enriched from
 *       {@code #KONTO} records; cost-centre/project from dimension objects</li>
 * </ol>
 *
 * <p>If no {@code #RAR} records are present, vouchers are grouped by calendar year.
 */
public class SieAccountingFileImporter implements AccountingFileImporter {

    @Override public String   getSystemName()       { return SieAdapter.SYSTEM_NAME; }
    @Override public String   getFormatName()       { return "SIE4"; }
    @Override public String   getFileDescription()  { return "SIE Files (*.si, *.se, *.sie)"; }
    @Override public String[] getFileExtensions()   { return new String[]{"si", "se", "sie"}; }
    @Override public String   getCountryCode()      { return "SE"; }

    // -------------------------------------------------------------------------
    // Main entry point
    // -------------------------------------------------------------------------

    @Override
    public AccountingFileData importFile(File file) throws Exception {
        SIEFileType4 sieFile = new SIEFileType4(file.getAbsolutePath());
        sieFile.readFile();

        BusinessPartner<?>          bp      = buildBusinessPartner(sieFile);
        List<AccountElement>        globalCoa = buildChartOfAccounts(sieFile);
        List<AccountingPeriodData> periods = buildPeriods(sieFile, globalCoa);

        AccountingFileData result = new AccountingFileData();
        result.setSourceFormat(getFormatName());
        result.setCountryCode(getCountryCode());
        result.setCompany(bp);
        result.setPeriods(periods);
        return result;
    }

    // -------------------------------------------------------------------------
    // BusinessPartner
    // -------------------------------------------------------------------------

    private BusinessPartner<?> buildBusinessPartner(SIEFileType4 sieFile) {
        BusinessPartner<?> bp = new BusinessPartner<>();
        bp.setName(sieFile.getFNamn());
        bp.setTaxId(sieFile.getOrgNr());
        bp.setCompany(true);
        bp.setActive(true);
        bp.setCountryCode("SE");   // SIE is a Swedish-only format
        return bp;
    }

    // -------------------------------------------------------------------------
    // Chart of accounts
    // -------------------------------------------------------------------------

    private List<AccountElement> buildChartOfAccounts(SIEFileType4 sieFile) {
        List<AccountElement> result = new ArrayList<>();
        Map<String, AccountRec> accountMap = sieFile.getAccountMap();
        if (accountMap != null) {
            // TreeMap is already sorted by account number
            for (AccountRec rec : accountMap.values()) {
                AccountElement ae = new AccountElement(rec.getAccountNo());
                ae.setName(rec.getAccountName());
                result.add(ae);
            }
        }
        return result;
    }

    // -------------------------------------------------------------------------
    // Period grouping
    // -------------------------------------------------------------------------

    private List<AccountingPeriodData> buildPeriods(SIEFileType4 sieFile,
                                                      List<AccountElement> globalCoa) {
        // Collect RAR records keyed by their offset so we can later match #IB/#UB records
        Map<Integer, RARRec> rarByOffset = new TreeMap<>();
        for (int offset = -10; offset <= 10; offset++) {
            RARRec rar = sieFile.getFiscalYear(offset);
            if (rar != null) rarByOffset.put(offset, rar);
        }

        // Sort by start date for a chronological period order
        List<Map.Entry<Integer, RARRec>> sortedRar = new ArrayList<>(rarByOffset.entrySet());
        sortedRar.sort(Comparator.comparing(e -> e.getValue().getStartDate()));

        List<RARRec> rarRecs = new ArrayList<>();
        Map<String, AccountingPeriodData> periodMap = new LinkedHashMap<>();
        Map<Integer, AccountingPeriodData> offsetToPeriod = new TreeMap<>();

        for (Map.Entry<Integer, RARRec> entry : sortedRar) {
            int    offset = entry.getKey();
            RARRec rar    = entry.getValue();
            rarRecs.add(rar);

            AccountingPeriodData period = rarToPeriod(rar);
            // Assign an independent deep copy of the chart of accounts to this period
            period.setChartOfAccounts(deepCopyCoa(globalCoa));
            periodMap.put(rarKey(rar), period);
            offsetToPeriod.put(offset, period);
        }

        // Assign vouchers to periods
        List<VerRec> verRecs = sieFile.getVerRecords();
        if (verRecs != null) {
            for (VerRec verRec : verRecs) {
                AccountingVoucher voucher = convertVoucher(verRec, sieFile);
                AccountingPeriodData period = findOrCreatePeriod(voucher, rarRecs, periodMap);
                period.getVouchers().add(voucher);
            }
        }

        // Populate opening / ending balances from #IB / #UB records
        Map<String, List<BalanceRec>> balanceMap = sieFile.getBalanceMap();
        if (balanceMap != null) {
            for (Map.Entry<Integer, AccountingPeriodData> entry : offsetToPeriod.entrySet()) {
                int                   offset = entry.getKey();
                AccountingPeriodData period = entry.getValue();
                for (AccountElement ae : period.getChartOfAccounts()) {
                    List<BalanceRec> recs = balanceMap.get(ae.getAccountNo());
                    if (recs == null) continue;
                    for (BalanceRec rec : recs) {
                        if (rec.getYearOffset() != offset) continue;
                        if (rec.isInBalance()) ae.setOpeningBalance(rec.getBalance());
                        else                   ae.setEndingBalance(rec.getBalance());
                    }
                }
            }
        }

        return new ArrayList<>(periodMap.values());
    }

    private List<AccountElement> deepCopyCoa(List<AccountElement> source) {
        List<AccountElement> copy = new ArrayList<>(source.size());
        for (AccountElement src : source) {
            AccountElement ae = new AccountElement(src.getAccountNo());
            ae.setName(src.getName());
            ae.setAccountClass(src.getAccountClass());
            ae.setTaxKey(src.getTaxKey());
            ae.setOpeningBalance(src.getOpeningBalance());
            ae.setEndingBalance(src.getEndingBalance());
            copy.add(ae);
        }
        return copy;
    }

    private AccountingPeriodData rarToPeriod(RARRec rar) {
        LocalDate start = rar.getStartDate().toLocalDate();
        LocalDate end   = rar.getEndDate().toLocalDate();

        AccountingPeriodData entry = new AccountingPeriodData();
        entry.setPeriodStart(start);
        entry.setPeriodEnd(end);
        // Name: "2024" for single-year, "2024-2025" for multi-year periods
        String name = start.getYear() == end.getYear()
            ? String.valueOf(start.getYear())
            : start.getYear() + "-" + end.getYear();
        entry.setName(name);
        entry.setVouchers(new ArrayList<>());
        return entry;
    }

    private String rarKey(RARRec rar) {
        return rar.getStartDate() + "/" + rar.getEndDate();
    }

    /**
     * Finds the period whose date range contains the voucher's acctDate.
     * Falls back to a calendar-year bucket if no RAR record matches.
     */
    private AccountingPeriodData findOrCreatePeriod(
            AccountingVoucher voucher,
            List<RARRec> rarRecs,
            Map<String, AccountingPeriodData> periodMap) {

        LocalDate date = voucher.getAcctDate();

        if (date != null) {
            for (RARRec rar : rarRecs) {
                LocalDate start = rar.getStartDate().toLocalDate();
                LocalDate end   = rar.getEndDate().toLocalDate();
                if (!date.isBefore(start) && !date.isAfter(end)) {
                    return periodMap.get(rarKey(rar));
                }
            }
        }

        // Fallback: group by calendar year
        int year = (date != null) ? date.getYear() : 0;
        String key = "year:" + year;
        return periodMap.computeIfAbsent(key, k -> {
            AccountingPeriodData entry = new AccountingPeriodData();
            if (date != null) {
                entry.setName(String.valueOf(year));
                entry.setPeriodStart(LocalDate.of(year, 1, 1));
                entry.setPeriodEnd(LocalDate.of(year, 12, 31));
            } else {
                entry.setName("Unknown");
            }
            entry.setVouchers(new ArrayList<>());
            return entry;
        });
    }

    // -------------------------------------------------------------------------
    // Voucher conversion
    // -------------------------------------------------------------------------

    private AccountingVoucher convertVoucher(VerRec verRec, SIEFileType4 sieFile) {
        AccountingVoucher voucher = new AccountingVoucher();
        voucher.setVoucherSeries(verRec.getSerie());
        voucher.setVoucherNo(verRec.getVerNr());
        voucher.setDescription(normalizeText(verRec.getVerText()));

        if (verRec.getVerDatum() != null) {
            voucher.setAcctDate(LocalDateUtils.asLocalDate(verRec.getVerDatum()));
        }
        if (verRec.getRegDatum() != null) {
            voucher.setRegDate(LocalDateUtils.asLocalDateTime(verRec.getRegDatum()));
        }

        if (verRec.getTransList() != null) {
            for (TransRec transRec : verRec.getTransList()) {
                // Skip #BTRANS / #RTRANS records — parser leaves kontoNr null for them
                if (transRec.getKontoNr() == null) continue;
                voucher.addVoucherLine(convertLine(transRec, sieFile));
            }
        }

        return voucher;
    }

    // -------------------------------------------------------------------------
    // Line conversion
    // -------------------------------------------------------------------------

    private AccountingVoucherLine convertLine(TransRec transRec, SIEFileType4 sieFile) {
        AccountingVoucherLine line = new AccountingVoucherLine();
        line.setAcctNo(transRec.getKontoNr());
        line.setDescription(normalizeText(transRec.getTransText()));

        // Enrich with account name from the chart of accounts
        Map<String, AccountRec> accountMap = sieFile.getAccountMap();
        if (accountMap != null && transRec.getKontoNr() != null) {
            AccountRec acct = accountMap.get(transRec.getKontoNr());
            if (acct != null) line.setAcctName(acct.getAccountName());
        }

        // Amount: positive in SIE = debit, negative = credit
        double amount = transRec.getBelopp();
        if (amount >= 0) line.setDebitAmount(BigDecimal.valueOf(amount));
        else             line.setCreditAmount(BigDecimal.valueOf(-amount));

        // Dimensions: cost centre (dim 1) and project (dim 6)
        if (transRec.getObjektLista() != null) {
            for (ObjRec obj : transRec.getObjektLista()) {
                if      (obj.getDimId() == DimRec.COSTCENTER_ID) line.setCostCenter(obj.getObjId());
                else if (obj.getDimId() == DimRec.PROJECT_ID)    line.setProjectCode(obj.getObjId());
            }
        }

        return line;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    /**
     * Normalises a raw SIE text field to {@code null} when it carries no real content.
     *
     * <p>The SIE parser sometimes leaves the surrounding double-quote delimiters in
     * place for empty or blank fields (e.g. the raw value is {@code ""} or {@code " "}).
     * After stripping outer quotes the remainder is checked for blankness.
     */
    private static String normalizeText(String text) {
        if (text == null) return null;
        String s = text.strip();
        // Strip a single layer of surrounding double-quotes if present
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            s = s.substring(1, s.length() - 1).strip();
        }
        return s.isEmpty() ? null : s;
    }
}
