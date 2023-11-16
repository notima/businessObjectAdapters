# Adempiere Adapter

This adapter is aimed to make it easy to connect an Adempiere database.

To connect the database a datasource needs to be added in the OSGI-environment.

As default the datasource expected is this

	(osgi.jndi.service.name=adempiere)
	
### Creating a datasource

An example datasource is created on installation. Modify this in the file

	$KARAF_HOME/etc/org.ops4j.datasource-adempiere.cfg
	
to suit your needs.
