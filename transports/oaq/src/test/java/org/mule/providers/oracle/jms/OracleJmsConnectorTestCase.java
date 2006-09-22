/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.oracle.jms;

import com.mockobjects.dynamic.Mock;

import org.mule.tck.providers.AbstractConnectorTestCase;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.TextMessage;

import java.util.Enumeration;

/**
 * @author <a href="mailto:carlson@hotpop.com">Travis Carlson </a>
 */
public class OracleJmsConnectorTestCase extends AbstractConnectorTestCase {

    private OracleJmsConnector connector;

    public UMOConnector getConnector() throws Exception {
        if (connector == null) {
            connector = new OracleJmsConnector();
            connector.setName("TestConnector");
            connector.setUrl("jdbc:oracle:oci:@TEST_DB");
            connector.setUsername("scott");
            connector.setPassword("tiger");

            Mock connectionFactory = new Mock(ConnectionFactory.class);
            Mock connection = new Mock(Connection.class);
            connectionFactory.expectAndReturn("createConnection", connection.proxy());
            connection.expect("close");
            connection.expect("start");
            connection.expect("stop");
            connection.expect("stop");
            connection.expect("setClientID", "mule.TestConnector");
            connector.setConnectionFactory((ConnectionFactory) connectionFactory.proxy());
            connector.initialise();
        }
        return connector;
    }

    public String getTestEndpointURI() {
        return "jms://TEST_QUEUE";
    }

    public Object getValidMessage() throws Exception {
        return getMessage();
    }

    public static Object getMessage() throws Exception {
        Mock message = new Mock(TextMessage.class);
        message.expectAndReturn("getText", "Test JMS Message");
        message.expectAndReturn("getText", "Test JMS Message");
        message.expectAndReturn("getJMSCorrelationID", null);
        message.expectAndReturn("getJMSMessageID", "1234567890");
        message.expectAndReturn("getJMSDeliveryMode", new Integer(1));
        message.expectAndReturn("getJMSDestination", null);
        message.expectAndReturn("getJMSPriority", new Integer(4));
        message.expectAndReturn("getJMSRedelivered", Boolean.FALSE);
        message.expectAndReturn("getJMSReplyTo", null);
        message.expectAndReturn("getJMSExpiration", new Long(0));
        message.expectAndReturn("getJMSTimestamp", new Long(0));
        message.expectAndReturn("getJMSType", null);
        message.expectAndReturn("getPropertyNames", new Enumeration() {

            public boolean hasMoreElements() {
                return false;
            }

            public Object nextElement() {
                return null;
            }
        });
        return message.proxy();
    }
}
