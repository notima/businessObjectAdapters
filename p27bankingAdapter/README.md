# P27 banking adapter

This adapter reads ISO 20022 bank files and converts them into payment batches, in the same way as the Adyen adapter.

Supported messages, all schema versions (001.02 up to the 2019 versions, 001.08 and later):

- camt.053 – bank statement (end of day)
- camt.054 – debit / credit notification
- camt.052 – account report (intraday)

The files are read with [iso20022lib](https://github.com/notima/iso20022lib), a library that is shared with the iDempiere plugin.

## Installation

```
feature:install notima-p27
```

The feature installs `iso20022lib` and this adapter. The adapter registers a `PaymentFactory` and a `PaymentBatchFactory` with the system name `P27`.

## Usage

A payment batch can be read from a single file or from a directory. In a directory, files ending with `.xml` or `.xml.txt` are read.

To list the payments as they are read from the files, without matching them against any destination:

```
list-payments-in-batch P27 /path/to/bankfiles
```

Add `-format xls` (requires the Excel adapter) to also write all payments to one file. The file includes the source file and payment type of each payment. The file name is given with `-of`, and defaults to the source with the format as suffix.

To process the payments in Fortnox:

```
process-payment-batch -d Fortnox P27 /path/to/Camt53_xxx.xml
```

Remove `-d` (dry run) to create the payments.

## Payment batches

Each account in a file results in up to two payment batches:

- **RECEIVABLE** – incoming payments (credit entries)
- **PAYABLE** – outgoing payments (debit entries)

A batch without payments is left out.

- **Booked entries only:** entries with another status, such as pending (`PDNG`) or information (`INFO`), are skipped.
- **One payment per transaction:** each transaction (`TxDtls`) of an entry becomes a payment. If the bank doesn't report any transactions for an entry, the entry itself becomes the payment.
- **Splitting:** a transaction that pays several invoices is split into one payment per invoice. This is only done when the amount per invoice is stated and the amounts add up to the transaction amount.
- **Reversals:** a reversed entry, such as a returned payment, is added with a negative amount to the batch of the original direction.
- **Payment date:** the booking date. If the entry has no booking date, the value date is used.
- **Payer or payee:** the debtor for incoming payments and the creditor for outgoing payments. The name and address go to the payment's business partner. The account (for instance a bankgiro number) goes to the payment's account number.
- **Transaction id:** the bank's reference (`AcctSvcrRef`) of the transaction. Split payments get the suffix `-1`, `-2` and so on.
- **Period:** taken from the statement's from and to dates.

## Reference matching

The reference used to find the invoice is chosen in this order:

| Source in the file | Example | Matched against |
|---|---|---|
| Referred document number (`RfrdDocInf/Nb`) | `107204` | `invoice` |
| Creditor reference (`CdtrRefInf/Ref`) | `8741054725` | `OCR` |
| Unstructured remittance (`Ustrd`) | `107208,` | `invoice` |

All references of a payment are also saved in the payment's comment.

Payers often write more than the invoice number, for instance `107193 2080` or `107208,`. Use `sourceReferenceRegex` to pick out the part to match. The first group is used.

## Configuration

Create a `p27.properties` file in the directory where the bank files are read from.

```
## If a p27.properties file is present in the bank file directory, these properties are used to create payment batches.
taxId=556000-0001
countryCode=SE
defaultCurrency=SEK
generalLedgerBankAccount=1930
generalLedgerInTransitAccount=1940
generalLedgerReconciliationAccount=1930
generalLedgerFeeAccount=6570
generalLedgerUnknownTrxAccount=2999
voucherSeries=B

## Optional. Use only the leading digits of the reference ("107193 2080" -> "107193").
sourceReferenceRegex=^(\\d+).*$

## Optional. Match all references against this field instead of the defaults above,
## for instance OCR, invoice or ExternalInvoiceReference1.
# destinationReference=OCR

## Optional. Regex applied to the destination system's reference before matching.
# destinationReferenceRegex=
```

The bank account number, IBAN and BIC of a batch are taken from the file. The general ledger accounts are taken from the properties.

## Limitations

- **Outgoing payments:** they are read into PAYABLE batches. The Fortnox payment batch processor only matches customer invoices, so these batches are not processed yet.
- **Payment files:** writing payment files (pain.001) is not supported yet.

## Tests

```
mvn test -pl p27bankingAdapter
```

The tests use fictional example files in `src/test/resources/camt`.

Real bank files can be put in `test-files`, which is ignored by git. If the directory exists, the tests also read those files. They check that the payments add up to the totals stated in each file.
