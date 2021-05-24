package org.notima.businessobjects.adapter.tools.command;

import java.lang.reflect.Field;

import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.lifecycle.Reference;

import org.apache.karaf.shell.api.console.Session;

public abstract class AbstractAction implements Action {

	@Reference
	protected Session sess;

    protected void updateMacros() throws IllegalArgumentException, IllegalAccessException{
        for(Field field  : this.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Argument.class)) {
                sess.put(((Argument)field.getAnnotation(Argument.class)).name(), field.get(this));
            }
        }
    }

	@Override
	public Object execute() throws Exception {	
        updateMacros();
        return onExecute();
    }

    protected abstract Object onExecute() throws Exception;
}
