package org.mule.test.integration.providers.jms.oracle.util;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.extras.client.MuleClient;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.w3c.dom.Document;

/**
 * Convenience methods for sending and receiving Mule messages.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class MuleUtil {

    public static final long MULE_RECEIVE_TIMEOUT = 10000;

    public static void sendXmlMessageToQueue(MuleClient muleClient, String queue, String xml) throws UMOException {
        sendXmlMessageToQueue(muleClient, queue, xml, /*correlationId*/null);
    }

    public static void sendXmlMessageToQueue(MuleClient muleClient, String queue, String xml, String correlationId) throws UMOException {
        HashMap messageProperties = null;
        if (correlationId != null) {
            messageProperties = new HashMap();
            messageProperties.put("MULE_CORRELATION_ID", correlationId);
        }
        muleClient.dispatch("jms://" + queue + "?transformers=StringToXMLMessage", xml, messageProperties);
    }

    public static String receiveXmlMessageAsString(MuleClient muleClient, String queue) throws UMOException {
        return (String) receiveMessage(muleClient, "jms://" + queue + "?"
                + OracleJmsConnector.PAYLOADFACTORY_PROPERTY + "=oracle.xdb.XMLTypeFactory", "JMSMessageToObject");
    }

    private static Object receiveMessage(MuleClient muleClient, String endpointUri, String transformers) throws UMOException {
        log.debug("Receiving message...");
        UMOMessage message = null;
        if (transformers != null) {
            message = muleClient.receive(endpointUri, transformers, MULE_RECEIVE_TIMEOUT);
        } else {
            message = muleClient.receive(endpointUri, MULE_RECEIVE_TIMEOUT);
        }
        if (message != null) {
            return message.getPayload();
        } else return null;
    }

    private static Log log = LogFactory.getLog(MuleUtil.class);
}
