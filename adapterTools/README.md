# Adapter Tools

This module contains commands and classes that work on the other modules in this library.

The logic is that the command specify which system/adapter (module) to call.

To see what adapters are enabled:

	list-adapters

List all tenants for a specific adapter

	list-tenants [adapter]

To list all customers for a specific adapter

	list-business-partners [adapter] [orgNo of tenant]

Copy tenant information from one adapter to another

	copy-tenant [srcAdapter] [dstAdapter] [orgNo of tenant]

## Tenant information

Tenant information is extra information about a tenant that isn't part of the tenant's adapter, ie where to write files and how the tenant gets paid. It's stored separately from the adapters, so a tenant in an adapter that can't store this kind of information (ie Adempiere) can still have it.

### Where it's stored

The tenant information is stored by a `TenantInformationFactory`. The jsonAdapter provides one (install the `notima-json` feature), which stores the information in `jsonAdapter/tenantInformation.json` in Karaf's home directory. The file can be changed with `tenantInformationFile` in `etc/jsonAdapterProperties.cfg`.

If more than one adapter provides a `TenantInformationFactory`, choose which one to use with `tenantInformationAdapter` (the adapter's system name) in `etc/AdapterTools.cfg`:

	tenantInformationAdapter = Json

Without the setting, the first one (by system name) is used and a warning is logged. The setting is read when the adapterTools bundle starts.

### Identifying the tenant

A tenant is identified by org number and country code. If the country code isn't given with `-co`, the `defaultCountryCode` in `etc/AdapterTools.cfg` is used (`SE` by default). Information stored with one country code isn't found with another.

### Setting tenant information

	set-tenant-info [orgNo] [attribute] [value]
	set-tenant-info -co SE 556745-6941 defaultOutputDirectory /home/user/karaf-output/notima

The entry is created if it doesn't exist. One attribute is set per command (tab completes the attribute names):

| Attribute | Description |
|---|---|
| `legalName` | The tenant's name, used in file names created by `read-invoices` |
| `taxId` | The org number |
| `countryCode` | The country code |
| `defaultOutputDirectory` | Where files are written when no file is given, ie by `read-invoices` and `show-canonical-invoice -of default` |
| `reportDirectory` | Where reports are written, ie payment channel match reports. If not set, `defaultOutputDirectory` is used |
| `remitToAccount` | The account invoices are paid to, ie a bankgiro number |
| `remitToAccountType` | The type of `remitToAccount`, ie `BG` (bankgiro) or `PG` (plusgiro) |
| `remitToIBAN` | IBAN invoices are paid to |
| `remitToBIC` | BIC for the IBAN |
| `lastDunningDate` | When reminders were last sent (`yyyy-MM-dd`, `none` to clear). Set by `print-dunning-run --internal-comment` |
| `suspenseAccounts.UNIDENTIFIED` | Suspense accounts for amounts we don't know what they are (OBS accounts, ie `1799,2999`) |
| `suspenseAccounts.IDENTIFIED` | Suspense accounts for amounts we believe we know what they are, ie double payments (ie `1684`) |
| `suspenseAccounts.OWNER_SETTLEMENT` | Settlement with the owner (ie `2893`) |

The suspense accounts are given separated by commas, `none` removes them. See [Suspense accounts](#suspense-accounts).

Note that changing `taxId` or `countryCode` changes how the entry is identified.

### Showing tenant information

	show-tenant-info [orgNo]
	show-tenant-info -co SE 556745-6941

Shows the stored information. If nothing is stored for the tenant, the registered adapters are searched for a tenant with the org number instead.

To see the output and report directory of all tenants in the adapters:

	list-tenants --with-info [adapter]

`list-tenants` lists the tenants in the adapters. A tenant only stored in the tenant information isn't listed.

### Where it's used

- `read-invoices` writes to `defaultOutputDirectory` if no file is given, named `[legalName or orgNo]-[yyyyMMdd].xml`.
- `show-canonical-invoice -of default` writes to `defaultOutputDirectory`, named `ar-invoice-[invoiceNo].xml` for sales invoices and `ap-invoice-[invoiceNo].xml` for vendor invoices.
- `read-invoices` sets the payment information (`remitTo...`) on the creditor of sales invoices. If `remitToAccount` or `remitToIBAN` is set in the tenant information, the tenant information's payment information overrides whatever the adapter supplied. All four `remitTo...` values are taken from the tenant information, also the ones not set, so they're never mixed with the adapter's. If neither has any, a warning is printed.
- Payment channel reports are written to `reportDirectory`, see [Report directory per tenant](#report-directory-per-tenant).
- `list-dunning-candidates` shows `lastDunningDate`, see [Queries over tenants](#queries-over-tenants).
- `list-suspense-balances` checks the `suspenseAccounts`, see [Suspense accounts](#suspense-accounts).

## Queries over tenants

These commands check many tenants at once and show one or more rows per tenant. They all take the arguments `[adapter] [orgNo...]`:

- Without arguments, all tenants of all adapters are checked.
- `adapter` limits the check to that adapter's tenants. If it isn't the name of an adapter but contains digits, it's taken as an org number, so the adapter can be left out.
- `orgNo` limits the check to these tenants: one or more org numbers, separated by spaces or commas. A requested org number that no adapter has is shown as `Not a tenant`.

Both arguments are tab completed (the org numbers of the given adapter's tenants). The other settings are options. `-co` sets the country code of all tenants checked (default: each tenant's own, else the default country code), and `-a` also shows the tenants with nothing to report. The progress is shown on one line while the tenants are checked, and a summary is printed after the table.

| Command | Shows |
|---|---|
| `list-dunning-candidates` | Tenants with overdue customer invoices, that need a dunning run. `-d` minimum days overdue, `--dunning-interval` days between reminders (tenants reminded more recently are only shown with `-a`) |
| `list-vendor-payment-candidates` | Tenants with vendor invoices to pay: overdue, or due within `-d` days (default 7) or by `--untildate`. Credit notes are deducted per supplier. Unbooked invoices are counted and shown in a column of their own; `--exclude-unbooked` and `--exclude-overdue` leave them out |
| `list-accounting-summary` | Revenue and result of the fiscal year until the date, equity, cash and bank, see [Accounting](#accounting) |
| `list-account-balances` | Balances of accounts or account categories, see [Accounting](#accounting) |
| `list-suspense-balances` | Suspense accounts that aren't zero, see [Suspense accounts](#suspense-accounts) |
| `refresh-accounting` | Fetches the accounting into the cache, see [Accounting](#accounting) |

	list-dunning-candidates Fortnox
	list-vendor-payment-candidates Fortnox 559169-1042 556000-0000 -d 14
	list-vendor-payment-candidates --untildate 2026-10-31

To see one tenant's open vendor invoices, with their due dates (`--show-references` also shows the order and reference columns):

	list-vendor-invoices [adapter] [orgNo]

## Accounting

The accounting of tenants (chart of accounts with balances, and vouchers) can be read from their accounting systems and queried, whatever the system. The model is format-neutral, from businessobjects (`AccountingFileData`), and balances and reports are calculated with businessobjects' `LedgerCalculator`, the same way as in accounting-analyzer.

### Where the accounting comes from

The accounting of a tenant is given by an `AccountingModelProvider` of its adapter's system:

| System | Provider | Requires |
|---|---|---|
| Fortnox | Reads the financial years of the client, and per year a SIE 4 export from Fortnox | The `notima-sie` feature, whose SIE importer reads the export |

### Accounts are classified by country

What an account is (asset, cash and bank, equity, revenue and so on) is its `AccountCategory`. The categories are set by the rules of the tenant's country (`CountryRuleSet`, for Sweden the BAS chart of accounts, ie 19xx is `ASSET_CASH`). The country is the one of the accounting data, else the tenant's, else the default country code. All queries go by the categories and never by account numbers, so that other countries' charts of accounts work once their rules are added.

The categories are `ASSET_LONG`, `ASSET_SHORT`, `ASSET_CASH`, `UNTAXED_RESERVE`, `LIABILITY_LONG`, `LIABILITY_SHORT`, `EQUITY`, `VAT_CLAIM`, `VAT_LIABILITY`, `TAX_LIABILITY`, `REVENUE`, `REVENUE_OTHER`, `COGS`, `OPERATING_EXPENSE`, `SALARY`, `IMPAIRMENTS`, `DEPRECIATIONS`, `OTHER_OPEX`, `FINANCIAL_INCOME`, `FINANCIAL_EXPENSE`, `TAX_EXPENSE` and `OTHER`.

### The cache

The accounting is cached, one file per system, tenant and fiscal year, in `${karaf.data}/accounting/cache/<system>/<orgNo>/<start of fiscal year>.json` (with `fiscal-years.json` for the list of fiscal years). A cached fiscal year is used for 24 hours before it's fetched again; change it with `accountingCacheMaxAgeHours` in `etc/AdapterTools.cfg` (`0` always fetches):

	accountingCacheMaxAgeHours = 12

The queries take `--refresh` to fetch the accounting even if it's cached. To fill or renew the cache, ie at night:

	refresh-accounting [adapter] [orgNo...]
	refresh-accounting Fortnox --all-years
	refresh-accounting Fortnox 559169-1042 --date 2025-12-31
	refresh-accounting --max-age-hours 20

By default the fiscal year that contains `--date` (today) is fetched; `--all-years` fetches all fiscal years. With `--max-age-hours`, fiscal years cached more recently are left as they are.

### Queries

A query reads the fiscal year that contains `--date` (default today), from its start until the date.

	list-accounting-summary [adapter] [orgNo...] [--date yyyy-MM-dd]

Per tenant: revenue and result of the year until the date, booked equity (without the result of the year until it's booked), cash and bank (`ASSET_CASH`), and the number and last date of the vouchers.

	list-account-balances [adapter] [orgNo...] --account 1930,19* [--date yyyy-MM-dd] [--non-zero]
	list-account-balances Fortnox --category ASSET_CASH,EQUITY

The balances of accounts (`--account`, a prefix ending with `*` sums the accounts starting with it) or categories (`--category`), one row per tenant and account, as booked (debit positive). The balance of a P&L account is its result from the start of the fiscal year. `--non-zero` only shows the balances that aren't zero.

### Reports per tenant

The report commands work for every tenant whose accounting a provider has, from the same cache:

	show-balance-report [orgNo] [--fromdate yyyy-MM-dd] [--untildate yyyy-MM-dd]
	show-pl-report [orgNo] [--fromdate yyyy-MM-dd] [--untildate yyyy-MM-dd]
	show-gl-details [orgNo] [accountNo] [--fromdate yyyy-MM-dd] [--untildate yyyy-MM-dd] [--orderbyamount]
	list-accounting-vouchers [orgNo] [series] [--fromdate yyyy-MM-dd] [--untildate yyyy-MM-dd]
	show-accounting-voucher [orgNo] [series] [voucherNo]

A report covers one fiscal year, the one that contains the end of the period asked for (else its start, else today). A missing start or end is the start or end of that fiscal year.

### Suspense accounts

Suspense accounts are accounts where amounts are booked temporarily, that should normally be zero. Which accounts they are varies between companies, so they're set per tenant in the tenant information, in three types:

| Type | Used for | Swedish example |
|---|---|---|
| `UNIDENTIFIED` | Amounts we don't know what they are (OBS account) | 1799, 2999 |
| `IDENTIFIED` | Amounts we believe we know what they are, but that aren't settled, ie double payments | 1684 |
| `OWNER_SETTLEMENT` | Settlement with the owner | 2893 |

	set-tenant-info 559169-1042 suspenseAccounts.UNIDENTIFIED 1799,2999
	set-tenant-info 559169-1042 suspenseAccounts.IDENTIFIED 1684
	set-tenant-info 559169-1042 suspenseAccounts.OWNER_SETTLEMENT 2893
	set-tenant-info 559169-1042 suspenseAccounts.IDENTIFIED none

They're stored in the tenant information file as `"suspenseAccounts": {"UNIDENTIFIED": ["1799", "2999"], ...}` and shown by `show-tenant-info`. To see the suspense accounts that aren't zero:

	list-suspense-balances [adapter] [orgNo...] [--type UNIDENTIFIED,OWNER_SETTLEMENT] [--date yyyy-MM-dd] [--include-zero]

One row per tenant and suspense account that isn't zero (rounded to cents), as booked (debit positive). Without `--type` all types are checked. Tenants without suspense accounts (of the types asked for) aren't read, and are only shown with `-a`, as `Not configured`. `--include-zero` also shows the accounts that are zero, and accounts that aren't in the tenant's chart of accounts.

## Printing invoices

`print-invoices` formats the invoices in a file created by `read-invoices` (xml) or a json invoice list, one file per invoice:

	print-invoices [-format pdf|peppol] [-od outputDirectory] [--delivery email|ekopost] [file]
	print-invoices -format peppol /home/user/karaf-output/notima/556745-6941-20261001.xml

Each file is named after the invoice's document key (with `-email` added for invoices sent by e-mail), and is written to `-od` or else the directory of the input file. The path of each file is printed.

| Format | Output | Provided by feature |
|---|---|---|
| `pdf` (default without delivery) | PDF | `notima-jasperreport` |
| `peppol` | E-invoice, UBL 2.1 Peppol BIS Billing 3.0 (`.xml`) | `notima-ubl` |

For `peppol`:
- Invoices with a negative total are written as a UBL CreditNote, with positive amounts.
- The seller is the invoice's sender. If the invoice has none, the creditor of the invoice list is used.
- The payment means (bankgiro, plusgiro or IBAN/BIC) come from the creditor's payment information. `read-invoices` puts it on the creditor, from the adapter or from the tenant information (`remitTo...`, see [Setting tenant information](#setting-tenant-information)). The payment reference is the invoice's OCR, or the invoice number if there is no OCR. Without payment information the e-invoice has no payment means and a warning is logged.

### Delivering invoices

With `--delivery`, each formatted invoice is also delivered to the customer. Without `-format`, the delivery method's format is used.

	print-invoices --delivery ekopost /home/user/karaf-output/notima/556745-6941-20261001.xml
	print-invoices -s -a test@example.com [file]

| Delivery | Format | Delivers | Provided by |
|---|---|---|---|
| `email` (also `-s`) | `pdf` | Sends each invoice right away to the customer's billing e-mail. Only customers that want e-mail invoices get one. `-a` sends all e-mails to that address instead (ie for testing). | adapterTools, using the e-mail message sender (`notima-email`) |
| `ekopost` | `peppol` | Queues all invoices in the file and sends them to Ekopost (Peppol) in one batch at the end. Uses the `Ekopost` configuration. | `ekopost-api` (notima-integration) |

For each invoice the result is printed (ie `sent to a@b.se`, `queued for Ekopost` or `not delivered by email`), and at the end the number of invoices delivered. An unknown delivery method gives an error listing the available ones.

A delivery method is an `InvoiceDeliveryMethod` service (`getType()`, `getDefaultFormat()`, `startBatch(...)`). The interfaces `InvoiceDeliveryMethod` and `InvoiceDeliveryBatch` are in businessobjects (`org.notima.generic.ifacebusinessobjects`), so new methods (ie Kivra) can be added in their own bundle without depending on adapterTools or changing `print-invoices`.

## Payment batches

Payment batches are a concept for reconciling payments. A payment batch here is a canonical format to represent a collection of payments with associated fees and payment transfer.

### Payment factory

A payment factory is a producer of payment batches. It takes the proprietary format of the factory implementation and formats it in a format readable by a payment batch processor.

The currently available payment processors can be listed using

	list-adapters
	
To use a payment factory one could use this command

	show-payment-batch [adapter] [source]
	
The format of the source is defined by the specific adapter used. Use the option --raw to see the actual payment batch in json.

### Payment batch processor

A payment batch processor is a consumer of payment batches. That means that it's responsible for applying the information in the payment batch to a target system and tenant.

List all payment batch processors

	list-payment-batch-processors
	
## Processing payments

Processing of payments involves both reading a payment batch from a payment factory and sending it to a payment batch processor.

One way of doing this is to use the command process-payment-batch

	process-payment-batch --options [destination] [paymentFactoryAdapter] [srcForPaymentFactoryAdapter]
	
- The destination system specifies to which payment batch processor the batch should be sent.
- The combined payment factory adapter and source creates a payment batch. The batch itself contains information about which tenant in the destination adapter they payments should be sent to.

## Payment channels

Payment channels are a way of defining payments to be processed.

	list-payment-batch-channels [orgNo]

### Thresholds

A channel can have thresholds that stop it from being processed when a report file contains too many unmatched payments. A payment is unmatched if no invoice is found for it, if the invoice found is already paid, or if the invoice couldn't be looked up (ie the destination system is unreachable).

Before a report file is processed, `process-payment-channel` matches its payments against the destination system (read only) and checks the thresholds. If a threshold is exceeded, the channel stops at that file: that file and all later files are left unprocessed in the source directory and "reconciled until" is not changed. The reason is printed after the processing report, for example:

	Channel stopped at 2026-09-25.json: 4 unmatched payments, limit 3
	The file and any later files were not processed. Use --force to process anyway.

There are three thresholds. Each is optional and applies to one report file (all currencies in the file together, except for the amount):

| Threshold | Option | Example |
|---|---|---|
| Max number of unmatched payments | `--max-unmatched` | `3` |
| Max share of unmatched payments, in percent | `--max-unmatched-percent` | `10` |
| Max unmatched amount per currency | `--max-unmatched-amount` | `5000` or `"5000,{500:EUR}"` |

The amount uses the same syntax as the general ledger accounts. A plain value applies to all currencies that aren't listed separately, so `"5000,{500:EUR}"` means 500 for EUR and 5000 for every other currency. If only separate currencies are listed (ie `"{500:EUR}"`), unmatched payments in any other currency exceed the threshold. Refunds count by their absolute amount.

#### Setting thresholds

Thresholds are set on the channel (and saved with it) using `modify-payment-channel`. The channel can be given by ID or description.

	modify-payment-channel --max-unmatched 3 [channelId]
	modify-payment-channel --max-unmatched-percent 10 --max-unmatched-amount "5000,{500:EUR}" [channelId]

Use `none` to remove a threshold:

	modify-payment-channel --max-unmatched none [channelId]

All values are validated before anything is changed on the channel.

#### Viewing thresholds

	show-payment-channel-status [channelId]

The rows `Max unmatched`, `Max unmatched %` and `Max unmatched amt` show the thresholds. `-` means no threshold.

#### Running with thresholds

- `process-payment-channel [channelId]` checks the thresholds of each report file before processing it.
- `process-payment-channel --dry-run [channelId]` checks the thresholds and reports where the channel would stop, but continues showing the payments and vouchers of all files.
- `process-payment-channel --force [channelId]` ignores the thresholds for this run, ie after checking the report.

A channel without thresholds is processed as before, without the extra lookup.

### Report directory per tenant

Reports are written to the tenant's report directory, which is stored in the tenant information:

	set-tenant-info [orgNo] reportDirectory /path/to/reports/tenant
	show-tenant-info [orgNo]

If `reportDirectory` isn't set, the tenant's `defaultOutputDirectory` is used. The tenant is identified by org number and country code (default from the AdapterTools settings, or `-co`). Channels stored without country code belong to the tenant with the same org number.

`process-payment-channel -format xls` writes its report to the tenant's report directory, unless an output file is given with `-of`. If the tenant has no report directory, the file is written relative to Karaf's working directory as before.

### Match report task

`PaymentChannelMatchReportTask` matches the pending report files of a tenant's **active** channels against their destination systems, the same way as `process-payment-channel --match-only`, and writes one report per channel to the tenant's report directory. Nothing is booked, no files are moved and the channels aren't changed.

- The task is created for one tenant (org number). Create one task per tenant.
- One report per channel with pending payments, named like the `-format` output of `process-payment-channel`, ie `AdyenSE_2026-09-28.json_260929.xls`. A run with the same pending files overwrites the previous report.
- Channels without pending payments, without source directory or with a missing adapter are skipped (and logged). A failing channel doesn't stop the others.
- If the tenant has no report directory, the task fails with a message telling how to set it. The directory is created if it doesn't exist.
- The format is `xls` by default (requires the excelAdapter).
- The task is locked per tenant, so the same tenant's task doesn't run twice at the same time. The locks are files in `${karaf.data}/task-locks`. A lock left by a Karaf process that no longer runs (ie after a restart) is ignored and removed.

Show and remove task locks:

	list-task-locks
	unlock-task payment-channel-match-report-556677-8899

The task can be run manually from the Karaf shell. It uses the same task lock as a scheduled run and prints its progress:

	run-match-report [orgNo]
	run-match-report -co SE --report-dir /tmp/reports -format xls [orgNo]
	run-match-report --until 2026-09-30 [orgNo]

	AdyenSE: 3 payments, 1 matched, 2 unmatched (66.67 %), unmatched amount SEK 325.00
	AdyenSE: thresholds: max unmatched 40 %, max unmatched amount 5000,{500:EUR}
	AdyenSE: processing would stop at 2026-09-28.json: 50.00 % unmatched payments (1 of 2), limit 40.00 %
	AdyenSE: report written to /path/to/reports/tenant/AdyenSE_2026-09-28.json_260929.xls
	1 active channels, 1 reports written to /path/to/reports/tenant
	Summary written to /path/to/reports/tenant/MatchSummary_556677-8899_260929.xls

	Channel │ Payments │ Matched │ Unmatched │ Unmatched % │ Currency │ Unmatched amt
	────────┼──────────┼─────────┼───────────┼─────────────┼──────────┼──────────────
	AdyenSE │        3 │       1 │         2 │       66,67 │ SEK      │        325,00

After the channel reports, a summary with one row per matched channel is written to the same directory (`MatchSummary_<orgNo>_<yyMMdd>.<format>`, replaced if the task runs again the same day). From the shell, the summary is also printed as a table.

For each channel the task states the matching result (unmatched means no invoice found, the invoice is already paid or couldn't be looked up), the channel's thresholds, and whether `process-payment-channel` would stop at one of the pending files (thresholds are checked per report file). From the shell this is printed to the console; when scheduled or run from a route it's written to the log.

Properties that can be set on the bean (the command's options set the same):

| Property | Default |
|---|---|
| `countryCode` | The default country code in the AdapterTools settings |
| `reportDirectory` | The tenant's report directory (overrides it if set) |
| `format` | `xls` |
| `untilDate` | None, all pending files are matched. With `--until yyyy-MM-dd`, only files dated until (and including) the date are matched, the same rule as `process-payment-channel --untildate` |

The task is created in a Blueprint file in Karaf's `deploy` directory and can be triggered either by a Camel route or by the Karaf scheduler.

Triggered by a Camel route, ie once a day:

	<bean id="matchReportTask" class="org.notima.businessobjects.adapter.tools.task.PaymentChannelMatchReportTask">
		<argument value="555555-5555"/>
	</bean>

	<camelContext id="payment-channel-match-report" xmlns="http://camel.apache.org/schema/blueprint">
		<route id="payment-channel-match-report">
			<from uri="timer://matchReport?delay=60000&amp;period=86400000"/>
			<to uri="bean:matchReportTask?method=run"/>
		</route>
	</camelContext>

Triggered by the Karaf scheduler (feature `scheduler`), ie every weekday at 07:00:

	<bean id="matchReportTask" class="org.notima.businessobjects.adapter.tools.task.PaymentChannelMatchReportTask">
		<argument value="555555-5555"/>
	</bean>

	<service ref="matchReportTask" interface="java.lang.Runnable">
		<service-properties>
			<entry key="scheduler.name" value="payment-channel-match-report-555555-5555"/>
			<entry key="scheduler.expression" value="0 0 7 ? * MON-FRI"/>
			<entry key="scheduler.concurrent" value="false"/>
		</service-properties>
	</service>

The format can be changed with `<property name="format" value="..."/>` on the bean, for any format a report formatter supports. Note that the excelAdapter always writes the xls format, also when asked for xlsx.
