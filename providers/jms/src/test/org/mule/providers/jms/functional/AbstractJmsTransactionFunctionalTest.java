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

import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleModel;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.jms.MessageRedeliveredException;
import org.mule.providers.jms.support.JmsTestUtils;
import org.mule.providers.jms.transformers.JMSMessageToObject;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.NamedTestCase;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.constraints.BatchConstraint;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOExceptionStrategy;
import org.mule.umo.UMOManager;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import javax.jms.JMSException;
import javax.jms.Message;
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

public abstract class AbstractJmsTransactionFunctionalTest extends NamedTestCase
{
    public static final String DEFAULT_IN_QUEUE = "jms://in.queue";
    public static final String DEFAULT_OUT_QUEUE = "jms://out.queue";
    public static final String DEFAULT_IN_TOPIC = "jms://topic:in.topic";
    public static final String DEFAULT_OUT_TOPIC = "jms://topic:out.topic";
    public static final String DEFAULT_MESSAGE = "Test Message";
    public static final String CONNECTOR_NAME = "testConnector";

    public static final long DEFAULT_TIMEOUT = 2000;
    
    private UMOConnector connector;
    private static UMOManager manager;
    private boolean callbackCalled = false;
    private UMOTransaction currentTx;
    private int eventCount = 0;
    private Connection cnn;
    private Object lock = new Object();

    protected void setUp() throws Exception
    {
        //By default the JmsTestUtils use the openjms config, though you can pass
        //in other configs using the property below
        if(MuleManager.isInstanciated()) MuleManager.getInstance().dispose();
        manager = MuleManager.getInstance();
        //Make sure we are running synchronously
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration().getPoolingProfile().setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        manager.setModel(new MuleModel());
        callbackCalled = false;
        currentTx = null;
        resetEventCount();
        cnn = getConnection();
        drainDestinations();
        connector = createConnector();
    }



    protected void drainDestinations() throws Exception
    {
        if(cnn instanceof QueueConnection)
        {
            JmsTestUtils.drainQueue((QueueConnection)cnn, DEFAULT_IN_QUEUE);
            assertNull(receive());
            JmsTestUtils.drainQueue((QueueConnection)cnn, DEFAULT_OUT_QUEUE);
            assertNull(receive());
        } else {
            JmsTestUtils.drainTopic((TopicConnection)cnn, DEFAULT_IN_TOPIC);
            assertNull(receive());
            JmsTestUtils.drainTopic((TopicConnection)cnn, DEFAULT_OUT_TOPIC);
            assertNull(receive());
        }
    }

    public abstract Connection getConnection() throws Exception;

    protected void tearDown() throws Exception
    {
        TransactionCoordination.getInstance().unbindTransaction();
        try
        {
            cnn.close();
        }
        catch (Throwable e) { }
        MuleManager.getInstance().dispose();
        
    }

    public void testSendNotTransacted() throws Exception
    {
        UMODescriptor descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
                synchronized(lock) {
                    lock.notify();
                }
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_NONE, UMOTransactionConfig.ACTION_NONE, callback);

        //Start the server
        MuleManager.getInstance().start();

        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);
        afterInitialise();
        synchronized(lock) {
            lock.wait(5000);
        }
        Message msg = receive();

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) msg).getText());
        assertTrue(callbackCalled);
        assertNull(currentTx);
    }

    public void testSendTransactedAlways() throws Exception
    {
        //setup the component and start Mule
        UMODescriptor descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                synchronized(lock) {
                    lock.notify();
                }
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        //Start the server
        MuleManager.getInstance().start();

        //Send a test message first so that it is there when the component is started
        send(DEFAULT_MESSAGE, false, getAcknowledgementMode());

        afterInitialise();
        synchronized(lock) {
            lock.wait(5000);
        }
        //Get the result message
        Message msg = receive();

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) msg).getText());
        assertTrue(callbackCalled);
        assertTrue(currentTx.isBegun());
        //todo for some reason this sometimes takes a little longer to commit??
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
        //setup the component and start Mule
        UMODescriptor descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

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
                synchronized(lock) {
                    lock.notify();
                }
            }
        };

        initialiseComponent(descriptor,
                (transactionAvailable ? UMOTransactionConfig.ACTION_ALWAYS_BEGIN : UMOTransactionConfig.ACTION_NONE),
                UMOTransactionConfig.ACTION_COMMIT_IF_POSSIBLE, callback);

        //Start the server
        MuleManager.getInstance().start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        afterInitialise();
        synchronized(lock) {
            lock.wait(5000);
        }
        //Get the result message
        Message msg = receive();

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) msg).getText());
        assertTrue(callbackCalled);

        if (transactionAvailable)
        {
            assertNotNull(currentTx);
            assertTrue(currentTx.isBegun());
            //todo for some reason this sometimes takes a little longer to commit??
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
        //This exception strategy will be invoked when a message is redelivered
        //after a rollback
        UMOExceptionStrategy es = new DefaultExceptionStrategy()
        {
            public Throwable handleException(Object message, Throwable t)
            {
                if (t instanceof MessageRedeliveredException)
                {
                    try
                    {
                        //MessageRedeliveredException mre = (MessageRedeliveredException)t;

                        TextMessage msg = (TextMessage) message;

                        assertNotNull(msg);
                        assertTrue(msg.getJMSRedelivered());
                        assertTrue(msg instanceof TextMessage);
                        assertEquals(DEFAULT_MESSAGE, msg.getText());
                    }
                    catch (Exception e)
                    {
                            fail(e.getMessage());
                    } finally {
                        try
                        {
                            //commit the message off the queue
                            ((MessageRedeliveredException) t).getSession().commit();
                        } catch (JMSException e)
                        {
                            fail("Failed to commit rolled back message");
                        }
                        synchronized(lock) {
                            lock.notify();
                        }
                    }
                    return null;
                }
                else
                {
                    t.printStackTrace();
                    synchronized(lock) {
                        lock.notify();
                    }
                    return null;
                }
            }
        };
        //setup the component and start Mule
        UMODescriptor descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                currentTx.setRollbackOnly();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        UMOManager manager = MuleManager.getInstance();

        UMOConnector umoCnn = manager.lookupConnector(CONNECTOR_NAME);
        //Set the test Exception strategy
        umoCnn.setExceptionStrategy(es);

        //Start the server
        manager.start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        afterInitialise();

        synchronized(lock) {
            lock.wait(5000);
        }
        //Get the result message, this should be null as the message was rolled back
        Message msg = receive();

        assertNull(msg);
        assertTrue(callbackCalled);
        assertTrue(currentTx.isRolledBack());
    }

    public void testSendBatchTransacted() throws Exception
    {
        //setup the component and start Mule
        UMODescriptor descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                eventCount++;
                if(eventCount==2) {
                    synchronized(lock) {
                        lock.notify();
                    }
                }
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);

        BatchConstraint c = new BatchConstraint();
        c.setBatchSize(2);
        descriptor.getInboundEndpoint().getTransactionConfig().setConstraint(c);
        //Start the server
        MuleManager.getInstance().start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE + "1", false, Session.AUTO_ACKNOWLEDGE);
        send(DEFAULT_MESSAGE + "2", false, Session.AUTO_ACKNOWLEDGE);

        afterInitialise();
        synchronized(lock) {
            lock.wait(5000);
        }
        //Get the result message
        Message msg = receive();

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + "1 Received", ((TextMessage) msg).getText());

        msg = receive();

        assertNotNull(msg);
        assertTrue(msg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + "2 Received", ((TextMessage) msg).getText());

        assertTrue(callbackCalled);
        assertEquals(2, eventCount);
        assertTrue(currentTx.isBegun());
        //todo for some reason this sometimes takes a little longer to commit??
        Thread.sleep(300);
        assertTrue(currentTx.isCommitted());
    }

    public void testSendBatchTransactedRollback() throws Exception
    {
        //This exception strategy will be invoked when a message is redelivered
        //after a rollback
        UMOExceptionStrategy es = new DefaultExceptionStrategy()
        {
            public Throwable handleException(Object message, Throwable t)
            {
                if (t instanceof MessageRedeliveredException)
                {
                    try
                    {
                        TextMessage msg = (TextMessage) message;

                        assertNotNull(msg);
                        assertTrue(msg.getJMSRedelivered());
                        assertTrue(msg instanceof TextMessage);
                        eventCount++;
                        assertEquals(DEFAULT_MESSAGE + eventCount, msg.getText());
                        if(eventCount==2) {
                            synchronized(lock) {
                                lock.notify();
                            }
                        }
                    }
                    catch (Exception e)
                    {
                        fail(e.getMessage());
                    } finally {
                        try
                        {
                            //commit the message off the queue
                            ((MessageRedeliveredException) t).getSession().commit();
                        } catch (JMSException e)
                        {
                            fail("Failed to commit rolled back message");
                        }
                    }
                    return null;
                }
                else
                {
                    return super.handleException(message, t);
                }
            }
        };
        //setup the component and start Mule
        UMODescriptor descriptor = getTestDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback()
        {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                incEventCount();
                if (eventCount == 2)
                {
                    currentTx.setRollbackOnly();
                    eventCount = 0;

                }

            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, UMOTransactionConfig.ACTION_ALWAYS_COMMIT, callback);
        List constraints = new ArrayList();
        BatchConstraint c = new BatchConstraint();
        c.setBatchSize(2);
        constraints.add(c);
        descriptor.getInboundEndpoint().getTransactionConfig().setConstraint(c);

        UMOManager manager = MuleManager.getInstance();

        UMOConnector umoCnn = manager.lookupConnector(CONNECTOR_NAME);
        //Set the test Exception strategy
        umoCnn.setExceptionStrategy(es);

        //Start the server
        manager.start();

        //Send a test message firstso that it is there when the component is started
        send(DEFAULT_MESSAGE + "1", false, Session.AUTO_ACKNOWLEDGE);
        send(DEFAULT_MESSAGE + "2", false, Session.AUTO_ACKNOWLEDGE);

        afterInitialise();

        synchronized(lock) {
            lock.wait(5000);
        }
        //Get the result message, this should be null as the message was rolled back
        Message msg = receive();
        assertNull(msg);
        assertTrue(callbackCalled);
        assertEquals(2, eventCount);
        assertTrue(currentTx.isRolledBack());

    }

    public void testCleanup() throws Exception
    {
        assertNull("There should be no transaction associated with this thread",TransactionCoordination.getInstance().unbindTransaction());
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
        MuleManager.getInstance().registerConnector(connector);
        return component;
    }

    public static UMODescriptor getTestDescriptor(String name, String implementation)
    {
        UMODescriptor descriptor = new MuleDescriptor();
        descriptor.setExceptionStrategy(new DefaultExceptionStrategy());
        descriptor.setName(name);
        descriptor.setImplementation(implementation);
        return descriptor;
    }

    public void afterInitialise() throws Exception
    {
        //Thread.sleep(1000);
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
        } catch (MalformedEndpointException e)
        {
            fail(e.getMessage());
            return null;
        }
    }

    protected void send(String payload, boolean transacted, int ack) throws JMSException
    {
        if(cnn instanceof QueueConnection) {
            JmsTestUtils.queueSend((QueueConnection)cnn, getInDest().getAddress(), payload, transacted, ack, null);
        } else {
            JmsTestUtils.topicPublish((TopicConnection)cnn, getInDest().getAddress(), payload, transacted, ack);
        }
    }

    protected Message receive() throws JMSException
    {
        Message msg = null;
        if(cnn instanceof QueueConnection) {
            msg = JmsTestUtils.queueReceiver((QueueConnection)cnn, getOutDest().getAddress(), DEFAULT_TIMEOUT);
        } else {
            msg = JmsTestUtils.topicSubscribe((TopicConnection)cnn, getOutDest().getAddress(), DEFAULT_TIMEOUT);
        }
        return msg;
    }

    protected int getAcknowledgementMode() {
        return Session.AUTO_ACKNOWLEDGE;
    }

    protected void resetEventCount() {
        eventCount = 0;
    }

    protected void incEventCount() {
        eventCount++;
    }

    public abstract UMOTransactionFactory getTransactionFactory();

    public abstract UMOConnector createConnector() throws Exception;
}