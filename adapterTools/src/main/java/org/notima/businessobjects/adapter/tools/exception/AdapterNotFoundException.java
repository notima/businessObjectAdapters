package org.notima.businessobjects.adapter.tools.exception;

public class AdapterNotFoundException extends Exception {

	private static final long serialVersionUID = -7293234758591247937L;

    public AdapterNotFoundException(String adapterName){
        super(adapterName);
    }	
	
}
