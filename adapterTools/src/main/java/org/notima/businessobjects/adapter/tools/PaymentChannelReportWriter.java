package org.notima.businessobjects.adapter.tools;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.notima.businessobjects.adapter.tools.table.GenericTable;
import org.notima.generic.businessobjects.Payment;
import org.notima.generic.businessobjects.PaymentBatch;
import org.notima.generic.businessobjects.TaxSubjectIdentifier;
import org.notima.generic.businessobjects.TenantInformation;
import org.notima.generic.ifacebusinessobjects.PaymentBatchChannel;
import org.notima.generic.ifacebusinessobjects.TenantInformationFactory;
import org.notima.util.LocalDateUtils;

/**
 * Writes the payments of a payment channel's batches as a report file, ie xls.
 * Shared by the process-payment-channel command and scheduled tasks.
 */
public class PaymentChannelReportWriter {

	/**
	 * Merges the payments of all batches into the first batch.
	 *
	 * @param batches	The batches.
	 * @return	The first batch, with the payments of all batches. Null if there are no batches.
	 */
	public static PaymentBatch mergeBatches(List<PaymentBatch> batches) {

		if (batches==null || batches.isEmpty()) return null;
		PaymentBatch pb = batches.get(0);
		if (pb.getPayments()==null) {
			List<Payment<?>> list = new ArrayList<Payment<?>>();
			pb.setPayments(list);
		}
		for (int i = 1 ; i<batches.size(); i++) {
			PaymentBatch add = batches.get(i);
			if (!add.isEmpty() && add.getPayments()!=null) {
				pb.getPayments().addAll(add.getPayments());
			}
		}
		return pb;

	}

	/**
	 * Groups batches by source (file). A file can result in more than one batch (ie one per currency).
	 * 
	 * @return	The batches per source, in the order they were given.
	 */
	public static Map<String, List<PaymentBatch>> groupBySource(List<PaymentBatch> batches) {
		Map<String, List<PaymentBatch>> result = new LinkedHashMap<String, List<PaymentBatch>>();
		if (batches==null) return result;
		for (PaymentBatch pb : batches) {
			List<PaymentBatch> fileBatches = result.get(pb.getSource());
			if (fileBatches==null) {
				fileBatches = new ArrayList<PaymentBatch>();
				result.put(pb.getSource(), fileBatches);
			}
			fileBatches.add(pb);
		}
		return result;
	}
	
	/**
	 * @return	The earliest first payment date of the batches, or null if none have payments.
	 */
	public static LocalDate getFirstPaymentDate(List<PaymentBatch> batches) {
		LocalDate result = null;
		for (PaymentBatch pb : batches) {
			result = earliest(result, LocalDateUtils.asLocalDate(pb.getFirstPaymentDate()));
		}
		return result;
	}
	
	/**
	 * @return	The first date of the file: the start of the period the report covers, or the first
	 * 			payment date, whichever is earlier. Null if neither is known.
	 */
	public static LocalDate getFileStartDate(List<PaymentBatch> batches) {
		LocalDate result = getFirstPaymentDate(batches);
		for (PaymentBatch pb : batches) {
			result = earliest(result, pb.getPeriodFrom());
		}
		return result;
	}
	
	/**
	 * Keeps the batches of the files dated until (and including) the until date, the same rule
	 * process-payment-channel uses. Files without a known date are left out.
	 *
	 * @param batches	The batches.
	 * @param untilDate	The until date. If null, all batches are kept.
	 * @return	The batches of the files to process.
	 */
	public static List<PaymentBatch> filterUntil(List<PaymentBatch> batches, LocalDate untilDate) {
		if (untilDate==null || batches==null) return batches;
		List<PaymentBatch> result = new ArrayList<PaymentBatch>();
		for (List<PaymentBatch> fileBatches : groupBySource(batches).values()) {
			LocalDate fileStartDate = getFileStartDate(fileBatches);
			if (fileStartDate!=null && !fileStartDate.isAfter(untilDate)) {
				result.addAll(fileBatches);
			}
		}
		return result;
	}
	
	private static LocalDate earliest(LocalDate a, LocalDate b) {
		if (a==null) return b;
		if (b==null) return a;
		return b.isBefore(a) ? b : a;
	}
	
	/**
	 * Creates the report's file name: channel description (or source system), the batch's source
	 * and, if the payments span several dates, the last payment date.
	 *
	 * @param channel	The channel.
	 * @param pb		The (merged) batch.
	 * @param format	The format, used as file extension.
	 * @return	The file name.
	 */
	public static String buildFileName(PaymentBatchChannel channel, PaymentBatch pb, String format) {

		String filePrefix =
				(channel.getChannelDescription()!=null && channel.getChannelDescription().trim().length()>0 ? channel.getChannelDescription() : channel.getSourceSystem());

		if (!pb.isDateRange()) {
			return filePrefix + "_" + pb.getSource() + "." + format;
		} else {
			return filePrefix + "_" + pb.getSource() + "_" + new SimpleDateFormat("yyMMdd").format(pb.getLastPaymentDate()) + "." + format;
		}

	}

	/**
	 * Writes a table as a report file.
	 *
	 * @param formatterFactory	To find the formatter for the format.
	 * @param table				The table to write.
	 * @param format			The format, ie xls.
	 * @param outputDir			The directory to write to. If null, the formatter's default is used.
	 * @param fileName			The file name.
	 * @return	The path of the written file. Null if there's no formatter for the format.
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	public static String writeReport(FormatterFactory formatterFactory, GenericTable table, String format, String outputDir, String fileName) throws Exception {

		ReportFormatter<GenericTable> rf = (ReportFormatter<GenericTable>) formatterFactory.getReportFormatter(GenericTable.class, format);
		if (rf==null) return null;

		Properties props = new Properties();
		props.setProperty(BasicReportFormatter.OUTPUT_FILENAME, fileName);
		if (outputDir!=null) {
			props.setProperty(BasicReportFormatter.OUTPUT_DIR, outputDir);
		}
		return rf.formatReport(table, format, props);

	}

	/**
	 * Checks if two identifiers are the same tenant: same tax id, and same country code if both have one.
	 * Channels are often stored without country code, while tenant information always has one.
	 */
	public static boolean isSameTenant(TaxSubjectIdentifier a, TaxSubjectIdentifier b) {
		if (a==null || b==null || !a.hasTaxId() || !b.hasTaxId()) return false;
		if (!a.getTaxId().trim().equalsIgnoreCase(b.getTaxId().trim())) return false;
		if (a.hasCountryCode() && b.hasCountryCode()) {
			return a.getCountryCode().trim().equalsIgnoreCase(b.getCountryCode().trim());
		}
		return true;
	}
	
	/**
	 * Finds the tenant's report directory (the tenant information's report directory, or its
	 * default output directory).
	 * 
	 * @param cof					To find the tenant information.
	 * @param tenant				The tenant. If it has no country code, the default country code is used.
	 * @param defaultCountryCode	The default country code. Can be null.
	 * @return	The report directory, or null if the tenant has none.
	 */
	public static String resolveReportDirectory(CanonicalObjectFactory cof, TaxSubjectIdentifier tenant, String defaultCountryCode) {
		
		if (cof==null || tenant==null || !tenant.hasTaxId()) return null;
		TenantInformationFactory tif = cof.lookupTenantInformationFactory();
		if (tif==null) return null;
		
		TenantInformation ti = null;
		if (!tenant.hasCountryCode() && defaultCountryCode!=null && defaultCountryCode.trim().length()>0) {
			ti = tif.getTenantInformation(new TaxSubjectIdentifier(tenant.getTaxId(), defaultCountryCode.trim()));
		}
		if (ti==null) {
			ti = tif.getTenantInformation(tenant);
		}
		return ti!=null ? ti.getEffectiveReportDirectory() : null;
		
	}
	
}
