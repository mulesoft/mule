/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.jms.functional;

import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.test.JmsTestUtils;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.provider.UMOConnector;
import org.mule.util.concurrent.Latch;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.QueueConnection;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;

public abstract class AbstractJmsFunctionalTestCase extends FunctionalTestCase
{
    public static final String DEFAULT_IN_QUEUE = "jms://in.q";
    public static final String DEFAULT_OUT_QUEUE = "jms://out.q";
    public static final String DEFAULT_DL_QUEUE = "jms://dlq";
    public static final String DEFAULT_IN_TOPIC = "jms://topic:in.t";
    public static final String DEFAULT_OUT_TOPIC = "jms://topic:out.t";
    public static final String DEFAULT_DL_TOPIC = "jms://topic:dlt";
    public static final String DEFAULT_MESSAGE = "Test Message";
    public static final String CONNECTOR_NAME = "testConnector";
    
    /** 
     * Default timeout for reading from a JMS queue/topic.  The higher this value, the more 
     * reliable the test will be, so it should be set high for Continuous Integration.  However, 
     * this can waste time during day-to-day development cycles, so you may want to temporarily 
     * lower it while debugging.  
     */
    public static final long JMS_TIMEOUT = 10000;
    
    protected JmsConnector connector;
    
    /** 
     * We use a direct JMS connection for unit testing.  Note that the MuleClient could be used 
     * for this, but not relying on it gives us better isolation for tracking down problems.
     */
    protected javax.jms.Connection connection;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        
        if (connector == null)
        {
            connector = lookupConnector();
        }
        
        // TODO Is this necessary?
        // Make sure we are running synchronously
        // RegistryContext.getConfiguration().setDefaultSynchronousEndpoints(true);

        // Reset the semaphore.
        callbackCalled = new Latch();
        
        // Open a direct JMS connection (outside of Mule) for testing the queues/topics.
        connection = getSenderConnection();
        connection.start();
    }

    protected void doTearDown() throws Exception
    {
        if (connection != null)
        {
            connection.close();
        }
    }

    public Connection getSenderConnection() throws Exception
    {
        ConnectionFactory cf = (ConnectionFactory) connector.getConnectionFactory().getOrCreate();
        return cf.createConnection();
    }

    protected void send(String payload, String dest) throws JMSException
    {
        send(payload, dest, false, getAcknowledgementMode(), null);
    }
    
    protected void send(String payload, String dest, boolean transacted, int ack, String replyTo) throws JMSException
    {
        if (useTopics())
        {
            JmsTestUtils.topicPublish((TopicConnection)connection, dest, payload, transacted, ack, replyTo);
        }
        else
        {
            JmsTestUtils.queueSend((QueueConnection)connection, dest, payload, transacted, ack, replyTo);
        }
    }

    protected String receiveTextMessage(String dest) throws JMSException
    {
        return receiveTextMessage(dest, JMS_TIMEOUT);
    }
    
    protected String receiveTextMessage(String dest, long timeout) throws JMSException
    {
        Message output = receive(dest, timeout);
        assertNotNull(output);
        assertTrue(output instanceof TextMessage);
        return ((TextMessage)output).getText();
    }
    
    protected Message receive(String dest) throws JMSException
    {
        return receive(dest, JMS_TIMEOUT);
    }

    protected Message receive(String dest, long timeout) throws JMSException
    {
        Message msg = null;
        if (useTopics())
        {
            msg = JmsTestUtils.topicSubscribe((TopicConnection)connection, dest, timeout);
        }
        else
        {
            msg = JmsTestUtils.queueReceiver((QueueConnection)connection, dest, timeout);
        }
        return msg;
    }

    public boolean useTopics()
    {
        return false;
    }
    
    protected int getAcknowledgementMode()
    {
        return Session.AUTO_ACKNOWLEDGE;
    }

    private JmsConnector lookupConnector()
    {
        UMOConnector connector = managementContext.getRegistry().lookupConnector("jmsConnector");
        assertNotNull("Connector not found in config", connector);
        assertTrue("Connector is not a JmsConnector", connector instanceof JmsConnector);
        return (JmsConnector) connector;
    }
}
