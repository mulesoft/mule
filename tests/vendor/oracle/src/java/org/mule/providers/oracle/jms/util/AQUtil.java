package org.mule.providers.oracle.jms.util;

import oracle.AQ.*;
import oracle.jms.AQjmsSession;

import javax.jms.JMSException;

/**
 * Convenience methods for creating and deleting Oracle queues.  This class uses
 * the native {@code oracle.AQ} package because administrative functionality is not 
 * available through the standard JMS package ({@code oracle.jms}).
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 * @see <a href="http://www.lc.leidenuniv.nl/awcourse/oracle/appdev.920/a96587/toc.htm">Oracle9i Application Developer's Guide - Advanced Queueing</a>
 */
public class AQUtil {

	public static void createOrReplaceTextQueue(AQjmsSession session, String schema, String name) throws AQException, JMSException {
		createOrReplaceQueue(session, schema, name, /*payloadType*/"SYS.AQ$_JMS_TEXT_MESSAGE");
	}
	
	public static void createOrReplaceXmlQueue(AQjmsSession session, String schema, String name) throws AQException, JMSException {
		createOrReplaceQueue(session, schema, name, /*payloadType*/"SYS.XMLTYPE");
	}
	
	public static void createOrReplaceQueue(AQjmsSession session, String schema, String name, String payloadType) throws AQException, JMSException {
	    dropQueue(session, schema, name, /*force*/true);
		createQueue(session, schema, name, payloadType);
	}
	
	public static void createQueue(AQjmsSession session, String schema, String name, String payloadType) throws AQException, JMSException {
   	   AQQueueTable table = session.createQueueTable (schema, "queue_" + name, new AQQueueTableProperty(payloadType));
   	   AQQueue queue = table.createQueue (name, new AQQueueProperty());
   	   queue.start();
   	} 

    public static void dropQueue(AQjmsSession session, String schema, String name) throws AQException, JMSException {
    	dropQueue(session, schema, "queue_" + name, /*force*/false);
    }
    
    public static void dropQueue(AQjmsSession session, String schema, String name, boolean force) throws AQException, JMSException {
    	AQQueueTable queueTable = null;
    	try {
     		queueTable = session.getQueueTable(schema, "queue_" + name);
     	} catch (JMSException e) {
     		// The queue does not exist.
     		if (force == false) throw e;
     	}

     	if (queueTable != null) {
     		queueTable.drop(/*stop and drop associated queues*/true);
     	}
   	} 
}
