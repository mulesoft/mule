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

import EDU.oswego.cs.dl.util.concurrent.CountDown;
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

    public static final long LOCK_WAIT = 20000;

    private UMOConnector connector;
    private static UMOManager manager;
    private boolean callbackCalled = false;
    private Connection cnn;
    private Message currentMsg;


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
        cnn = getConnection();
        cnn.start();
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
        final CountDown countDown = new CountDown(2);

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
                countDown.release();
            }
        };

        initialiseComponent(callback);
        //Start the server
        MuleManager.getInstance().start();

        MessageConsumer mc;
        //check replyTo
        if(useTopics()) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection)cnn, getOutDest().getAddress());
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection)cnn, getOutDest().getAddress());
        }
        mc.setMessageListener(new MessageListener(){
            public void onMessage(Message message)
            {
                currentMsg = message;
                countDown.release();
            }
        });
        afterInitialise();
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, null);
        assertTrue(countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());

        assertTrue(callbackCalled);
    }


    public void testSendWithReplyTo() throws Exception
    {
        final CountDown countDown = new CountDown(2);
        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
                countDown.release();
            }
        };

        initialiseComponent(callback);
        //Start the server
        MuleManager.getInstance().start();

        Message msg = null;

        MessageConsumer mc;
        //check replyTo
        if(useTopics()) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection)cnn, "replyto");
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection)cnn, "replyto");
        }
        mc.setMessageListener(new MessageListener(){
            public void onMessage(Message message)
            {
                currentMsg = message;
                countDown.release();
            }
        });

        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE, "replyto");
        afterInitialise();

        assertTrue(countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);
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
            if(!useTopics()) {
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
            if(!useTopics()) {
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
        if(!useTopics()) {
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

    public boolean useTopics() {
        return false;
    }

    public abstract UMOConnector createConnector() throws Exception;
}
