package org.notima.generic.pgp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * An entity that allows pgp public key look up by user id (email address).
 * 
 * @author Oliver Norin
 *
 */
@Entity
public class PGPKey {
    @Id
	@GeneratedValue
	private Long keyId;
    private String keyFileLocation;
    private String userId;

    public Long getKeyId(){
        return keyId;
    }

    public void setKeyId(Long keyId){
        this.keyId = keyId;
    }

    public String getKeyFileLocation(){
        return keyFileLocation;
    }

    public void setKeyFileLocation(String keyFileLocation){
        this.keyFileLocation = keyFileLocation;
    }

    public String getUserId(){
        return userId;
    }

    public void setUserId(String userId){
        this.userId = userId;
    }
}
