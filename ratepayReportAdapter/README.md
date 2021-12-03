# Ratepay Adapter

This adapter adds the capability to parse files from Ratepay.

## Installation in Karaf

	repo-add mvn:org.notima.generic.businessobjects.adapter/adapterTools/LATEST/xml/features
	
	feature:install notima-ratepay
	
## Example usage

	show-payment-batch -d Ratepay /path/file
	
## Testing

"Live" test-reports can be placed in the src/test/resources/reports folder a.k.a reports folder. Only files with csv-endings are considered.

In the reports folder a ratepay.properties file is expected. This file describes who the report files belong to since that information isn't directly derivable from the csv-files.

### TestRatepayDirectoryToPaymentBatch

Tests reading reports from the "reports" folder and converts them into PaymentBatches.

