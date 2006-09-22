/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.oracle.util;

import java.util.HashMap;

import org.mule.extras.client.MuleClient;
import org.mule.providers.oracle.jms.OracleJmsConnector;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

/**
 * Convenience methods for sending and receiving Mule messages.
 *
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson</a>
 */
public class MuleUtil {

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

    public static String receiveXmlMessageAsString(MuleClient muleClient, String queue, long timeout) throws UMOException {
        UMOMessage msg = muleClient.receive("jms://" + queue + "?"
                + OracleJmsConnector.PAYLOADFACTORY_PROPERTY + "=oracle.xdb.XMLTypeFactory", timeout);
        return (msg != null ? (String) msg.getPayload() : null);
    }
}
