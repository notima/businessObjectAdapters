# fortnoxAdapter
Adapter to deal with Fortnox

## Installation in Karaf

	repo-add mvn:org.notima.generic.businessobjects.adapter/adapterTools/LATEST/xml/features
	
	feature:install notima-fortnox4j
	
## Configuration

The default client secret can be viewed and/or configured with below commands

```
 	karaf@root()> config:property-list -p FortnoxProperties 
   		defaultClientSecret = DONT_STORE_HERE
   		fortnoxClientsFile = private/fortnoxClients.xml
   		org.apache.karaf.features.configKey = FortnoxProperties
```

Set the default client secret

	config:property-set -p FortnoxProperties defaultClientSecret THE_SECRET

Restart the Fortnox adapter when the secret has been set

### Adding clients

Add new clients using the command

	fortnox:add-client
	
If there's no defaultClientSecret, that must be supplied with the clientSecret option.

If you already have existing clients and your Fortnox connection has access to CompanySettings you can add your clients with this command

	fortnox:add-client --accessToken REAL_ACCESS_TOKEN --clientSecret REAL_SECRET
	
or if you've set the defaultClientSecret

	fortnox:add-client --accessToken REAL_ACCESS_TOKEN
	
### List clients

List clients with

	fortnox:list-fortnox-clients

### Removing clients

Remove client using the command

	fortnox:remove-client NNNNNN-NNNN
	
where NN is the org number.