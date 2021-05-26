package org.notima.businessobjects.adapter.tools.command.annotation.processor;

public class BasicConfirmationProcessor implements ConfirmationProcessor {

    @Override
    public String getConfirmationMessage(String argName, Object value, String messageFormat) {
        return String.format(messageFormat, argName, value.toString());
    }
    
}
