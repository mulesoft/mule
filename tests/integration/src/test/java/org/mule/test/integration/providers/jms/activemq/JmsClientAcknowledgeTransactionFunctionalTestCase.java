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

import org.mule.providers.jms.JmsClientAcknowledgeTransactionFactory;
import org.mule.providers.jms.JmsConnector;
import org.mule.umo.UMOTransactionFactory;

import javax.jms.Session;

/**
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

//todo check ActiveMqJmsTransactionFunctionalTest

public class JmsClientAcknowledgeTransactionFunctionalTestCase extends ActiveMQJmsTransactionFunctionalTestCase
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
