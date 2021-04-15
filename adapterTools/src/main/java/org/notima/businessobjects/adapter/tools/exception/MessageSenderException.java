package org.notima.businessobjects.adapter.tools.exception;

public class MessageSenderException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public MessageSenderException(String message, Throwable cause){
        super(message, cause);
    }
    
    public MessageSenderException(String message){
        super(message);
    }
}
