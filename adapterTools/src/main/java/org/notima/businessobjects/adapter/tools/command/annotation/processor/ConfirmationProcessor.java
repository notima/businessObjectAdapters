package org.notima.businessobjects.adapter.tools.command.annotation.processor;

public interface ConfirmationProcessor {
    public String getConfirmationMessage(String argName, Object value, String messageFormat);
}
