/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.cxf.issues;

import static javax.jms.DeliveryMode.NON_PERSISTENT;
import static javax.jms.Session.AUTO_ACKNOWLEDGE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.size.SmallTest;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQBytesMessage;
import org.junit.Before;
import org.junit.Test;

@SmallTest
public class CfxClientRaceConditionTestCase extends FunctionalTestCase
{
    private static final String AMDS_CXF_JAX_WS_CLIENT_EXPECTED_MESSAGE =
            "<soap:Envelope xmlns:soap=\"http://www.w3.org/2003/05/soap-envelope\"><soap:Body/></soap:Envelope>";

    private static final String INBOUND_TEST_QUEUE = "inboundTest";
    
    private static final String OUTBOUND_TEST_QUEUE = "cxfTest";
    
    private static final String ACTIVEMQ_CONNECTION_ADDRESS = "vm://localhost";
    
    private int totalMessages;

    @Before
    public void setup()
    {
        totalMessages = 10000;
    }

    @Test
    public void testRaceCondition() throws Exception
    {
        cxfJmsTest(INBOUND_TEST_QUEUE, OUTBOUND_TEST_QUEUE, AMDS_CXF_JAX_WS_CLIENT_EXPECTED_MESSAGE);
    }

    private void cxfJmsTest(String inboundQueue, String outboundQueue, String expectedMessage) throws Exception
    {
        ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(ACTIVEMQ_CONNECTION_ADDRESS);

        Connection consumerConnection = connectionFactory.createConnection();
        consumerConnection.start();
        Session consumerSession = consumerConnection.createSession(false, AUTO_ACKNOWLEDGE);
        Destination consumerDestination = consumerSession.createQueue(outboundQueue);
        MessageConsumer consumer = consumerSession.createConsumer(consumerDestination);

        Connection producerConnection = connectionFactory.createConnection();
        producerConnection.start();
        Session producerSession = producerConnection.createSession(false, AUTO_ACKNOWLEDGE);
        Destination producerDestination = producerSession.createQueue(inboundQueue);
        MessageProducer producer = producerSession.createProducer(producerDestination);
        producer.setDeliveryMode(NON_PERSISTENT);

        long errorCount = 0L;
        for (int i = 0; i < totalMessages; i++)
        {
            BytesMessage messageSend = producerSession.createBytesMessage();
            producer.send(messageSend);
            Message messageReceived = consumer.receive(5000);
            ActiveMQBytesMessage bytesMessage = (ActiveMQBytesMessage) messageReceived;
            String messageBody = new String(bytesMessage.getContent().getData());
            String expected = expectedMessage;
            String messageTrimmed = messageBody.trim();

            if (expected.length() != messageTrimmed.length())
            {
                errorCount++;
            }
        }

        assertThat(errorCount, equalTo(0L));

        producer.close();
        producerSession.close();
        producerConnection.close();

        consumer.close();
        consumerSession.close();
        consumerConnection.close();
    }

    @Override
    protected String getConfigResources()
    {
        return "issues/cfx-client-race-condition.xml";
    }

}
