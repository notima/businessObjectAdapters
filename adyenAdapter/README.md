# Adyen adapter

This adapter reads settlement detail files and converts those into payment batches.

The payments in a payment batch gets the same date as the merchant payout date. This is to allow for some more time to
pass if the invoices hasn't been created the same time as the payment was settled.

The reason for choosing the merchant payout date is also to be able to see that the payout corresponds to the total of 
the payments and associated fees.

Configuration of the adapter is done by creating an adyen.properties file in the same directory where the payment batches are read from.

Example of the file is down below

```
## If an adyen.properties file is present in the reports directory, these properties are used to create payment batches.
taxId=555555-5555
countryCode=NL
defaultCurrency=EUR
generalLedgerReconciliationAccount=1587
generalLedgerFeeAccount=6060
generalLedgerUnknownTrxAccount=1521
generalLedgerInTransitAccount=1953
voucherSeries=AD
```

