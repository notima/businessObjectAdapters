package org.notima.businessobjects.adapter.tools.command;

import java.lang.reflect.Field;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import org.apache.karaf.shell.api.console.Session;
import org.notima.businessobjects.adapter.tools.command.annotation.Confirmation;

/**
 * Abstract implementation of Karaf shell api action.
 * This superclass intercepts command line arguments
 * passed to inherited commands and stores them for
 * later reference as shell variables.
 */
public abstract class AbstractAction implements Action {

	protected DateFormat dfmt = new SimpleDateFormat("yyyy-MM-dd");	
	
    @Reference
    protected Session sess;

    /**
     * Check for confirmation annotations and show appropriate confirmation
     * messages.
     * 
     * @return 
     * true if the user confirms the action or if no confirmation annotation 
     * is present.
     * 
     * @throws Exception
     */
    protected boolean confirm() throws Exception {
        boolean hasConfiramtion = false;
        for(Field field  : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Confirmation.class)) {
                Confirmation annotation = field.getAnnotation(Confirmation.class);
                Class<?> confirmationClass = field.getAnnotation(Confirmation.class).type();
                org.notima.businessobjects.adapter.tools.command.annotation.processor.ConfirmationProcessor confirmation = 
                        (org.notima.businessobjects.adapter.tools.command.annotation.processor.ConfirmationProcessor) confirmationClass.newInstance();
                sess.getConsole().println(confirmation.getConfirmationMessage(field.getName(), field.get(this), annotation.messageFormat()));
                hasConfiramtion = true;
            }
        }
        if(hasConfiramtion){
            String reply = sess.readLine("Are you sure you want to continue? (y/n) ", null);
            return reply.equalsIgnoreCase("y");
        }else{
            return true;
        }
    }

    /**
     * assigns each field with the @Argument annotation to a shell variable.
     * @throws Exception
     */
    protected void updateMacros() throws Exception{
        for(Field field  : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Argument.class)) {
                field.setAccessible(true);
                if(field.isAccessible()) {
                    sess.put(((Argument)field.getAnnotation(Argument.class)).name(), field.get(this));
                } else {
                    throw new Exception(field.getName() + " is inaccessible");
                }
            }
        }
    }

    /**
     * Called by the Karaf shell when the command is executed.
     */
    @Override
    public Object execute() throws Exception {
        if(!confirm()){
            sess.getConsole().println("Command execution cancelled");
            return null;
        } 
        updateMacros();
        return onExecute();
    }

    /**
     * Custom behaviour of the subclass.
     * @return
     * @throws Exception
     */
    protected abstract Object onExecute() throws Exception;
}
