/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jms.functional;

import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.impl.MuleModel;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOManager;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import java.util.HashMap;

/**
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractJmsFunctionalTestCase extends AbstractMuleTestCase
{
    public static final String DEFAULT_IN_QUEUE = "jms://in.q";
    public static final String DEFAULT_OUT_QUEUE = "jms://out.q";
    public static final String DEFAULT_IN_TOPIC = "jms://topic:in.t";
    public static final String DEFAULT_OUT_TOPIC = "jms://topic:out.t";
    public static final String DEFAULT_MESSAGE = "Test Message";
    public static final String CONNECTOR_NAME = "testConnector";

    private UMOConnector connector;
    private static UMOManager manager;
    private boolean callbackCalled = false;
    private int eventCount = 0;
    private Connection cnn;
    private Message currentMsg;
    private Object lock = new Object();

    protected void setUp() throws Exception
    {
        //By default the JmsTestUtils use the openjms config, though you can pass
        //in other configs using the property below

        manager = MuleManager.getInstance();
        //Make sure we are running synchronously
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().getPoolingProfile().setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        manager.setModel(new MuleModel());
        callbackCalled = false;
        eventCount = 0;
        cnn = getConnection();
        drainDestinations();
        connector = createConnector();
        MuleManager.getInstance().registerConnector(connector);
        currentMsg = null;
    }



    protected void drainDestinations() throws Exception
    {
        if(cnn instanceof QueueConnection)
        {
            JmsTestUtils.drainQueue((QueueConnection)cnn, DEFAULT_IN_QUEUE);
            //assertNull(receive(1000));
            JmsTestUtils.drainQueue((QueueConnection)cnn, DEFAULT_OUT_QUEUE);
            //assertNull(receive(1000));
        } else {
            JmsTestUtils.drainTopic((TopicConnection)cnn, DEFAULT_IN_TOPIC);
            //assertNull(receive(1000));
            JmsTestUtils.drainTopic((TopicConnection)cnn, DEFAULT_OUT_TOPIC);
            //assertNull(receive(5000));
        }
    }

    public abstract Connection getConnection() throws Exception;

    protected void tearDown() throws Exception
    {
        MuleManager.getInstance().dispose();
        try
        {
            cnn.close();
        }
        catch (JMSException e) { }
    }

    public void testSend() throws Exception
    {
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
            }
        };

        initialiseComponent(callback);
        //Start the server
        MuleManager.getInstance().start();

        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, null);
        afterInitialise();

        Message msg = receive();

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) msg).getText());
        assertTrue(callbackCalled);
    }


    public void testSendWithReplyTo() throws Exception
    {
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
            }
        };

        initialiseComponent(callback);
        //Start the server
        MuleManager.getInstance().start();

        Message msg = null;

        MessageConsumer mc;
        if(cnn instanceof TopicConnection) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection)cnn, getOutDest().getAddress());
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection)cnn, getOutDest().getAddress());
        }
        mc.setMessageListener(new MessageListener(){
            public void onMessage(Message message)
            {
                currentMsg = message;
                lock.notifyAll();
            }
        });
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, "replyto");
        afterInitialise();

        synchronized(lock) {
        lock.wait(20000);
        }
        msg = currentMsg;

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) msg).getText());
        assertTrue(callbackCalled);

        //check replyTo
        if(cnn instanceof TopicConnection) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection)cnn, "replyto");
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection)cnn, "replyto");
        }
        mc.setMessageListener(new MessageListener(){
            public void onMessage(Message message)
            {
                currentMsg = message;
                lock.notifyAll();
            }
        });
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, "replyto");
        afterInitialise();

        synchronized(lock) {
        lock.wait(20000);
        }
        msg = currentMsg;

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) msg).getText());
    }

    public void initialiseComponent(EventCallback callback) throws Exception
    {
        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        HashMap props = new HashMap();
        props.put("eventCallback", callback);

        builder.registerComponent(
                FunctionalTestComponent.class.getName(),
                "testComponent", getInDest(), getOutDest(), props);
    }

    public void afterInitialise() throws Exception
    {

    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            if(cnn instanceof QueueConnection) {
                return new MuleEndpointURI(DEFAULT_IN_QUEUE);
            } else {
                return new MuleEndpointURI(DEFAULT_IN_TOPIC);
            }
        } catch (MalformedEndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try
        {
            if(cnn instanceof QueueConnection) {
                return new MuleEndpointURI(DEFAULT_OUT_QUEUE);
            } else {
                return new MuleEndpointURI(DEFAULT_OUT_TOPIC);
            }
        } catch (Exception e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected void send(String payload, boolean transacted, int ack, String replyTo) throws JMSException
    {
        if(cnn instanceof QueueConnection) {
            JmsTestUtils.queueSend((QueueConnection)cnn, getInDest().getAddress(), payload, transacted, ack, replyTo);
        } else {
            JmsTestUtils.topicPublish((TopicConnection)cnn, getInDest().getAddress(), payload, transacted, ack, replyTo);
        }
    }

    protected Message receive() throws JMSException
    {
        Message msg = null;
        if(cnn instanceof QueueConnection) {
            msg = JmsTestUtils.queueReceiver((QueueConnection)cnn, getOutDest().getAddress(), 20000);
        } else {
            msg = JmsTestUtils.topicSubscribe((TopicConnection)cnn, getOutDest().getAddress(), 20000);
        }
        return msg;
    }

    public abstract UMOConnector createConnector() throws Exception;
}
