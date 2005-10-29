package org.mule.test.integration.providers.jms.oracle.util;

import java.io.ByteArrayInputStream;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOException;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.w3c.dom.Document;

/**
 * Convenience methods for sending and receiving Mule messages.
 * 
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class MuleUtil {

    public static void sendXmlMessageToQueue(String queue, String xml) throws UMOException {
    	sendMessage("jms://" + queue + "?transformers=StringToXMLMessage", xml);
    }

    public static String receiveXmlMessageAsString(String queue) throws UMOException {
		return (String) receiveMessage("jms://" + queue + "?"
				+ OracleJmsConnector.PAYLOADFACTORY_PROPERTY + "=oracle.xdb.XMLTypeFactory", 
				"XMLMessageToString");
	}
    
    public static Document receiveXmlMessageAsDOM(String queue) throws UMOException {
		return (Document) receiveMessage("jms://" + queue + "?"
				+ OracleJmsConnector.PAYLOADFACTORY_PROPERTY + "=oracle.xdb.XMLTypeFactory", 
				"XMLMessageToDOM");
	}
    
    public static void sendMessage(String endpointUri, Object data) throws UMOException {
    	sendMessage(endpointUri, data, /*properties*/null);
    }
	
    public static void sendMessage(String endpointUri, Object data, Map properties) throws UMOException {
        log.debug("Sending message...");
        MuleClient client = new MuleClient();
        try {
            client.dispatch(endpointUri, data, properties);
        } finally {
            client.dispose(); 
        }
    }
    
    public static void sendMessageAsStream(String endpointUri, String data) throws UMOException {
    	sendMessage(endpointUri, new ByteArrayInputStream(data.getBytes()));
    }

    public static Object receiveMessage(String endpointUri) throws UMOException {
    	return receiveMessage(endpointUri, /*transformers*/null);
    }
    
    public static Object receiveMessage(String endpointUri, String transformers) throws UMOException {
        log.debug("Receiving message...");
        MuleClient client = new MuleClient();
        Object message = null;
        try {
        	if (transformers != null) {
        		message = client.receive(endpointUri, transformers, 10000).getPayload();
        	} else {
        		message = client.receive(endpointUri, 10000).getPayload();
        	}
        } finally {
            client.dispose(); 
        }
        return message;
    }        

    private static Log log = LogFactory.getLog(MuleUtil.class);
}
