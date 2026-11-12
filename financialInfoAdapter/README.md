# Financial Info Adapter

Financial information from open sources, as a `FinancialInfoProvider` of businessobjects:

* **Exchange rates** (`ExchangeRateProvider`): Sveriges Riksbank (SWEA API, about 30 currencies against SEK), with the ECB's euro reference rates for what Riksbanken lacks or when it can't be reached. Cross rates are worked out from either. The rate on a day is the latest published on or before it.
* **Interest rates** (`InterestRateProvider`): Riksbanken's reference rate (`SE_REF_RATE`) and policy rate (`SE_POLICY_RATE`).

More kinds of information (ie index numbers) are added as capability interfaces of `FinancialInfoProvider`.

## Use

* **accounting-analyzer**: add the plugin (`org.notima.generic.businessobjects.adapter:financialInfoAdapter`); posting in a foreign currency then gets its rate from here.
* **Karaf**: `feature:install notima-financialinfo` registers the service.

## Settings

Optional, in `~/.notima/financialinfo.properties` (or the file of the system property `notima.financialinfo.config`):

```
sources = riksbank, ecb
riksbank.subscriptionKey =
cacheDir = ~/.notima/financialinfo/cache
timeoutSeconds = 20
```

Without a subscription key Riksbanken allows 5 calls a minute and 1000 a day per IP address; a free key from <https://developer.api.riksbank.se> raises that. Rates of past days are cached on disk, so a day is fetched once per source.

Riksbanken's rates are indicative; it asks to be named as the source ("Source: Sveriges Riksbank"), which each result does.
