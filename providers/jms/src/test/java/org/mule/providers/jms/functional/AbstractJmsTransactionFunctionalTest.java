/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *
 */

package org.mule.providers.jms.functional;

import EDU.oswego.cs.dl.util.concurrent.CountDown;
import org.mule.MuleManager;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.jms.MessageRedeliveredException;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.providers.jms.transformers.JMSMessageToObject;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.constraints.BatchConstraint;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOManager;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.QueueConnection;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.TopicConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * <code>AbstractJmsTransactionFunctionalTest</code> is a base class for all jms based
 * functional tests with or without transactions.
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */

public abstract class AbstractJmsTransactionFunctionalTest extends AbstractJmsFunctionalTestCase
{

    protected UMOTransaction currentTx;

    protected void setUp() throws Exception
    {
        super.setUp();
        currentTx = null;
    }


    protected void tearDown() throws Exception
    {
        TransactionCoordination.getInstance().unbindTransaction();
        super.tearDown();
    }

    public void testSendNotTransacted() throws Exception
    {
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

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

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_NONE, UMOTransactionConfig.ACTION_NONE, callback);
        addResultListener(getOutDest().getAddress(), countDown);
        MuleManager.getInstance().start();
        afterInitialise();
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);
        assertTrue("Only " + countDown.currentCount() + " of " + countDown.initialCount() + " checkpoints hit",
                countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);
        assertNull(currentTx);
    }

    public void testSendTransactedAlways() throws Exception
    {
        final CountDown countDown = new CountDown(2);
        //setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                countDown.release();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        //Start the server
        MuleManager.getInstance().start();
        addResultListener(getOutDest().getAddress(), countDown);

        //Send a test message first so that it is there when the component is started
        send(DEFAULT_MESSAGE, false, getAcknowledgementMode());

        assertTrue("Only " + countDown.currentCount() + " of " + countDown.initialCount() + " checkpoints hit",
                countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);
        assertTrue(currentTx.isBegun());
        //todo for some reason, it takes a while for committed flag on the tx to update
        Thread.sleep(300);
        assertTrue(currentTx.isCommitted());

    }

    public void testSendTransactedIfPossibleWithTransaction() throws Exception
    {
        doSendTransactedIfPossible(true);
    }

    public void testSendTransactedIfPossibleWithoutTransaction() throws Exception
    {
        doSendTransactedIfPossible(false);
    }

    private void doSendTransactedIfPossible(final boolean transactionAvailable) throws Exception
    {
        final CountDown countDown = new CountDown(2);
        //setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                if (transactionAvailable)
                {
                    assertNotNull(currentTx);
                    assertTrue(currentTx.isBegun());
                }
                else
                {
                    assertNull(currentTx);
                }
                countDown.release();
            }
        };

        initialiseComponent(descriptor,
                (transactionAvailable ? UMOTransactionConfig.ACTION_ALWAYS_BEGIN : UMOTransactionConfig.ACTION_NONE),
                UMOTransactionConfig.ACTION_COMMIT_IF_POSSIBLE, callback);

        //Start the server
        MuleManager.getInstance().start();
        addResultListener(getOutDest().getAddress(), countDown);

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        assertTrue("Only " + countDown.currentCount() + " of " + countDown.initialCount() + " checkpoints hit",
                countDown.attempt(LOCK_WAIT));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);

        if (transactionAvailable)
        {
            assertNotNull(currentTx);
            assertTrue(currentTx.isBegun());
            //todo for some reason, it takes a while for committed flag on the tx to update
            Thread.sleep(300);
            assertTrue(currentTx.isCommitted());
        }
        else
        {
            assertNull(currentTx);
        }
    }

    public void testSendTransactedRollback() throws Exception
    {
        final CountDown countDown = new CountDown(2);
        //This exception strategy will be invoked when a message is redelivered
        //after a rollback

        //setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                System.out.println("@@@@ Rolling back transaction @@@@");
                currentTx.setRollbackOnly();
                countDown.release();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        UMOManager manager = MuleManager.getInstance();
        addResultListener(getOutDest().getAddress(), countDown);

        UMOConnector umoCnn = manager.lookupConnector(CONNECTOR_NAME);
        //Set the test Exception strategy
        umoCnn.setExceptionStrategy(new RollbackExceptionHandler(countDown));

        //Start the server
        manager.start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);
        afterInitialise();
        assertTrue("Only " + countDown.currentCount() + " of " + countDown.initialCount() + " checkpoints hit",
                countDown.attempt(LOCK_WAIT));

        assertNull(currentMsg);
        assertTrue(callbackCalled);
        assertTrue(currentTx.isRolledBack());

        //Make sure the message isn't on the queue
        assertNull(receive(getInDest().getAddress(), 2000));
    }

    public void testSendBatchTransacted() throws Exception
    {
        final CountDown countDown = new CountDown(4);
        //setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                eventCount++;
                countDown.release();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        addResultListener(getOutDest().getAddress(), countDown);
        BatchConstraint c = new BatchConstraint();
        c.setBatchSize(2);
        descriptor.getInboundEndpoint().getTransactionConfig().setConstraint(c);
        //Start the server
        MuleManager.getInstance().start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE + "1", false, Session.AUTO_ACKNOWLEDGE);

        assertTrue(!countDown.attempt(1000));

        send(DEFAULT_MESSAGE + "2", false, Session.AUTO_ACKNOWLEDGE);

        assertTrue("Only " + countDown.currentCount() + " of " + countDown.initialCount() + " checkpoints hit",
                countDown.attempt(LOCK_WAIT));
        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + "2 Received", ((TextMessage) currentMsg).getText());

        assertTrue(callbackCalled);
        assertEquals(2, eventCount);
        assertTrue(currentTx.isBegun());
        //todo for some reason, it takes a while for committed flag on the tx to update
        Thread.sleep(300);
        assertTrue(currentTx.isCommitted());
    }

    public void testSendBatchTransactedRollback() throws Exception
    {
        final CountDown countDown = new CountDown(4);

        //setup the component and start Mule

        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                eventCount++;
                if (eventCount == 2)
                {
                    currentTx.setRollbackOnly();
                    eventCount = 0;
                }
                countDown.release();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        addResultListener(getOutDest().getAddress(), null);
        List constraints = new ArrayList();
        BatchConstraint c = new BatchConstraint();
        c.setBatchSize(2);
        constraints.add(c);
        descriptor.getInboundEndpoint().getTransactionConfig().setConstraint(c);

        UMOManager manager = MuleManager.getInstance();

        UMOConnector umoCnn = manager.lookupConnector(CONNECTOR_NAME);
        //Set the test Exception strategy
        umoCnn.setExceptionStrategy(new RollbackExceptionHandler(countDown));

        //Start the server
        manager.start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE + "1", false, Session.AUTO_ACKNOWLEDGE);

        assertTrue(!countDown.attempt(1000));
        send(DEFAULT_MESSAGE + "2", false, Session.AUTO_ACKNOWLEDGE);

        assertTrue("Only " + countDown.currentCount() + " of " + countDown.initialCount() + " checkpoints hit",
                countDown.attempt(LOCK_WAIT));

        assertNull(currentMsg);
        assertTrue(callbackCalled);
        assertTrue(currentTx.isRolledBack());
    }

    public void testCleanup() throws Exception
    {
        assertNull("There should be no transaction associated with this thread", TransactionCoordination.getInstance().unbindTransaction());
    }

    public UMOComponent initialiseComponent(UMODescriptor descriptor, byte txBeginAction, byte txCommitAction,
                                            EventCallback callback) throws Exception
    {
        JMSMessageToObject inTrans = new JMSMessageToObject();
        ObjectToJMSMessage outTrans = new ObjectToJMSMessage();

        UMOEndpoint endpoint = new MuleEndpoint("testIn", getInDest(), connector, inTrans,
                UMOEndpoint.ENDPOINT_TYPE_RECEIVER, null);

        UMOTransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setFactory(getTransactionFactory());
        txConfig.setBeginAction(txBeginAction);

        UMOEndpoint outProvider = new MuleEndpoint("testOut", getOutDest(), connector, outTrans,
                UMOEndpoint.ENDPOINT_TYPE_SENDER, null);

        UMOTransactionConfig txConfig2 = new MuleTransactionConfig();
        txConfig2.setCommitAction(txCommitAction);

        endpoint.setTransactionConfig(txConfig);
        outProvider.setTransactionConfig(txConfig2);

        descriptor.setOutboundEndpoint(outProvider);
        descriptor.setInboundEndpoint(endpoint);
        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        descriptor.setProperties(props);
        UMOComponent component = MuleManager.getInstance().getModel().registerComponent(descriptor);
        //MuleManager.getInstance().registerConnector(connector);
        return component;
    }

    public static UMODescriptor getDescriptor(String name, String implementation)
    {
        UMODescriptor descriptor = new MuleDescriptor();
        descriptor.setExceptionStrategy(new DefaultExceptionStrategy());
        descriptor.setName(name);
        descriptor.setImplementation(implementation);
        return descriptor;
    }

    public void afterInitialise() throws Exception
    {
        Thread.sleep(1000);
    }

    protected UMOEndpointURI getInDest()
    {
        try
        {
            if (cnn instanceof QueueConnection)
            {
                return new MuleEndpointURI(DEFAULT_IN_QUEUE);
            } else
            {
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
            if (cnn instanceof QueueConnection)
            {
                return new MuleEndpointURI(DEFAULT_OUT_QUEUE);
            } else
            {
                return new MuleEndpointURI(DEFAULT_OUT_TOPIC);
            }
        } catch (MalformedEndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected void send(String payload, boolean transacted, int ack) throws JMSException
    {
        if (cnn instanceof QueueConnection)
        {
            JmsTestUtils.queueSend((QueueConnection) cnn, getInDest().getAddress(), payload, transacted, ack, null);
        } else
        {
            JmsTestUtils.topicPublish((TopicConnection) cnn, getInDest().getAddress(), payload, transacted, ack);
        }
    }

    protected int getAcknowledgementMode()
    {
        return Session.AUTO_ACKNOWLEDGE;
    }

    protected void addResultListener(String dest, final CountDown countDown) throws JMSException
    {
        MessageConsumer mc;
        //check replyTo
        if (useTopics())
        {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection) cnn, dest);
        } else
        {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection) cnn, dest);
        }
        mc.setMessageListener(new MessageListener()
        {
            public void onMessage(Message message)
            {
                currentMsg = message;
                if (countDown != null) countDown.release();
            }
        });
    }

    public abstract UMOTransactionFactory getTransactionFactory();

    public abstract UMOConnector createConnector() throws Exception;

    private class RollbackExceptionHandler extends DefaultExceptionStrategy
    {
        private CountDown countDown;

        public RollbackExceptionHandler(CountDown countDown)
        {
            this.countDown = countDown;
        }

        public Throwable handleException(Object message, Throwable t)
        {
            System.out.println("@@@@ ExceptionHandler Called @@@@");
            if (t instanceof MessageRedeliveredException)
            {
                countDown.release();
                try
                {
                    //MessageRedeliveredException mre = (MessageRedeliveredException)t;

                    TextMessage msg = (TextMessage) message;

                    assertNotNull(msg);
                    assertTrue(msg.getJMSRedelivered());
                    assertTrue(msg instanceof TextMessage);
                    //assertEquals(DEFAULT_MESSAGE, msg.getText());
                } catch (Exception e)
                {
                    fail(e.getMessage());
                } finally
                {
                    try
                    {
                        //commit the message off the queue
                        System.out.println("@@@@ committing rolled back message from queue @@@@");
                        ((MessageRedeliveredException) t).getSession().commit();
                    } catch (JMSException e)
                    {
                        fail("Failed to commit rolled back message");
                    }
                }
                return null;
            } else
            {
                t.printStackTrace();
                fail(t.getMessage());
                return null;
            }
        }
    }
}