package org.notima.businessobjects.adapter.tools.command;

import java.lang.reflect.Field;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import org.apache.karaf.shell.api.console.Session;

/**
 * Abstract implementation of Karaf shell api action.
 * This superclass intercepts command line arguments
 * passed to inherited commands and stores them for
 * later reference as shell variables as long as their
 * values are accessible to this class. private 
 * argument variables will not be saved since their
 * values are not accessible.
 */
public abstract class AbstractAction implements Action {

    @Reference
    protected Session sess;

    /**
     * assigns each field with the @Argument annotation to a shell variable.
     * As long as they are accessible (protected or public)
     * @throws IllegalArgumentException
     * @throws IllegalAccessException
     */
    protected void updateMacros() throws IllegalArgumentException, IllegalAccessException{
        for(Field field  : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Argument.class) && field.isAccessible()) {
                sess.put(((Argument)field.getAnnotation(Argument.class)).name(), field.get(this));
            }
        }
    }

    /**
     * Called by the Karaf shell when the command is executed.
     */
    @Override
    public Object execute() throws Exception {	
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