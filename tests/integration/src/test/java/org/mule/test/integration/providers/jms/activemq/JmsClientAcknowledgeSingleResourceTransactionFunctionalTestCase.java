/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms.activemq;

import java.util.HashMap;
import java.util.Map;

import javax.jms.Session;

import org.mule.providers.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.TransactedSingleResourceJmsMessageReceiver;
import org.mule.umo.UMOTransactionFactory;

public class JmsClientAcknowledgeSingleResourceTransactionFunctionalTestCase extends
    ActiveMQJmsTransactionFunctionalTestCase
{

    public UMOTransactionFactory getTransactionFactory()
    {
        return new JmsClientAcknowledgeTransactionFactory();
    }

    public JmsConnector createConnector() throws Exception
    {
        JmsConnector connector = new JmsConnector();
        connector.setName(CONNECTOR_NAME);
        connector.setAcknowledgementMode(Session.CLIENT_ACKNOWLEDGE);
        connector.getDispatcherThreadingProfile().setDoThreading(false);
        Map overrides = new HashMap();
        overrides.put("message.receiver", TransactedSingleResourceJmsMessageReceiver.class.getName());
        overrides.put("transacted.message.receiver",
            TransactedSingleResourceJmsMessageReceiver.class.getName());

        connector.setServiceOverrides(overrides);

        return connector;
    }

    protected int getAcknowledgementMode()
    {
        return Session.CLIENT_ACKNOWLEDGE;
    }

    public void testSendTransactedRollback() throws Exception
    {
        // Rollback not allowed for client acknowledge
    }

    public void testTransactedRedeliveryToDLDestination() throws Exception
    {
        // messages are not marked for redelivery in Client Ack mode
    }
}
