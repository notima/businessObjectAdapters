package org.notima.generic.pgp.command;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Dictionary;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.apache.karaf.shell.api.console.Session;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import me.sniggle.pgp.crypt.PGPKeyPairGenerator;

//import me.sniggle.pgp.crypt.PGPKeyPairGenerator;

@Service
@Command(scope = "notima", name = "generate-keypair", description = "Generates GPG keypair for email encryption. Default values from EmailProperties.cfg will be used if flags are omitted.")
public class GenerateKeyPair implements Action {

    private static final String EMAIL_CONFIG_FILE = "EmailProperties";
    private static final String PROP_PRIVATE_KEY_FILE = "senderPrivateKey";
    private static final String PROP_PRIVATE_KEY_PASS = "senderPrivateKeyPassword";
    private static final String PROP_PUBLIC_KEY_FILE = "senderPublicKey";
    private static final String PROP_PGP_USER_ID = "emailUser";
    
    @Reference
    private Session sess;

	@Reference
    private ConfigurationAdmin configAdmin;

    @Option(name = "-pu", aliases = { "--public-key-file" }, description = "Override the public key file location.", required = false, multiValued = false)
    private String publicKeyFile;

    @Option(name = "-pr", aliases = { "--private-key-file" }, description = "Override the private key file location.", required = false, multiValued = false)
    private String privateKeyFile;

    @Option(name = "-pa", aliases = { "--private-key-password" }, description = "Override the private key password.", required = false, multiValued = false)
    private String privateKeyPass;

    @Option(name = "-u", aliases = { "--user-id" }, description = "Override the user id.", required = false, multiValued = false)
    private String userId;

    @Override
    public Object execute() throws Exception {
        if(publicKeyFile == null)
            publicKeyFile = getProperty(PROP_PUBLIC_KEY_FILE);
        if(privateKeyFile == null)
            privateKeyFile = getProperty(PROP_PRIVATE_KEY_FILE);
        if(privateKeyPass == null)
            privateKeyPass = getProperty(PROP_PRIVATE_KEY_PASS);
        if(userId == null)
            userId = getProperty(PROP_PGP_USER_ID);
            
        new PGPKeyPairGenerator().generateKeyPair(
            userId, 
            privateKeyPass, 
            new FileOutputStream(new File(publicKeyFile)), 
            new FileOutputStream(new File(privateKeyFile)));

        sess.getConsole().printf("private key : %s\npublic key  : %s\n", privateKeyFile, publicKeyFile);
        sess.getConsole().println("keypair generated");
        return null;
    }

    /**
     * Get a dictionary of all properties in the WebpayEmailProperties configuration file
     * @return
     * @throws IOException
     */
	private Dictionary<String, Object> getProperties() throws IOException {
		Configuration configuration;
        try {
            configuration = configAdmin.getConfiguration(EMAIL_CONFIG_FILE);
        } catch (IOException e) {
            throw new IOException("Could not access the configuration file %s.cfg", e);
        }
		return configuration.getProperties();
    }

    /**
     * Get a property from the WebpayEmailProperties configuration file
     * @param key The key of the property
     * @return The property value
     * @throws Exception
     */
    private String getProperty(String key) throws Exception{
        String property;
        try {
            property = (String) getProperties().get(key);
        } catch (NullPointerException e) {
            throw new Exception(
                String.format(
                    "The value of property: %s in config file %s.cfg could not be retrieved.",
                    key,
                    EMAIL_CONFIG_FILE
                ),
                e
            );
        }
        return property;
    }
}

