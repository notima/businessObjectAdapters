package org.notima.generic.pgp;

import java.io.FileNotFoundException;

public class KeyNotFoundException extends Exception {
    
    private static final long serialVersionUID = 344721255053073311L;

    public KeyNotFoundException(String message, FileNotFoundException cause){
        super(message, cause);
    }
    
    public KeyNotFoundException(String message){
        super(message);
    }
}
