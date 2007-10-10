/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.providers.jms.integration;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

/**
 * Send and recieve JmsMessage without any tx
 */
public class JmsSingleTransactionNoneTestCase extends AbstractJmsFunctionalTestCase
{
    protected String getConfigResources()
    {
        return "providers/activemq/jms-single-tx-NONE.xml";
    }

    public void testNoneTx() throws Exception
    {
        send(scenarioNonTx);
        receive(scenarioNonTx);
        receive(scenarioNotReceive);
    }

    Scenario scenarioNonTx = new AbstractScenario()
    {
        public void send(Session session, MessageProducer producer) throws JMSException
        {
            producer.send(session.createTextMessage(DEFAULT_INPUT_MESSAGE));
        }

        public Message receive(Session session, MessageConsumer consumer) throws JMSException
        {
            Message message = consumer.receive(TIMEOUT);
            assertNotNull(message);
            return message;
        }

        public boolean isTransacted()
        {
            return false;
        }
    };


}
