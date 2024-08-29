package org.notima.businessobjects.adapter.tools.db;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class PersistenceManager {

	   private static SessionFactory sessionFactory;

	    static {
	        try {
	            sessionFactory = new Configuration().configure().buildSessionFactory();
	        } catch (Throwable ex) {
	            throw new ExceptionInInitializerError(ex);
	        }
	    }

	    public static Session getSession() {
	        return sessionFactory.openSession();
	    }

	    public static void closeSessionFactory() {
	        sessionFactory.close();
	    }	
	
}
