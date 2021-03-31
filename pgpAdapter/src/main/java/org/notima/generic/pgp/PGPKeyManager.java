package org.notima.generic.pgp;

import java.io.File;
import java.util.List;

/**
 * A service template that manages pgp keys and allows key look up by email address.
 * 
 * @author Oliver Norin
 *
 */
public interface PGPKeyManager {

    public PGPKey add(PGPKey key);
    public PGPKey update(PGPKey key);
    public PGPKey remove(PGPKey key);
    public PGPKey get(String userId);

    public File getSenderPublicKey();
    public File getSenderPrivateKey();
    public String getSenderPrivateKeyPassword();

    /**
     * List all stored keys
     * @return
     */
    public List<PGPKey> list();
}
