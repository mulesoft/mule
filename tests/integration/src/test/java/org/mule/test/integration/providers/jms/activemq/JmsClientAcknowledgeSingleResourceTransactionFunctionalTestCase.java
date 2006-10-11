/*
 * $Id: JmsClientAcknowledgeTransactionFunctionalTestCase.java 2181 2006-06-04 23:09:23Z holger $
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.jms.activemq;

import java.util.HashMap;
import java.util.Map;

import org.mule.providers.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.TransactedSingleResourceJmsMessageReceiver;
import org.mule.umo.UMOTransactionFactory;

import javax.jms.Session;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision: 2181 $
 */

//todo check ActiveMqJmsTransactionFunctionalTest

public class JmsClientAcknowledgeSingleResourceTransactionFunctionalTestCase extends ActiveMQJmsTransactionFunctionalTestCase
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
        overrides.put("transacted.message.receiver", TransactedSingleResourceJmsMessageReceiver.class.getName());
        
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
