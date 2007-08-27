/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jms;

import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

import org.mule.config.MuleProperties;
import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleTransactionConfig;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.MessageRedeliveredException;
import org.mule.providers.jms.transformers.JMSMessageToObject;
import org.mule.providers.jms.transformers.ObjectToJMSMessage;
import org.mule.tck.functional.EventCallback;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.transaction.TransactionCoordination;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;
import org.mule.umo.UMOTransactionFactory;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.endpoint.UMOEndpointURI;

import org.mule.umo.provider.UMOConnector;
import org.mule.util.concurrent.CountDownLatch;

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
 * <code>AbstractJmsTransactionFunctionalTest</code> is a base class for all
 * jms based functional tests with or without transactions.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */

public abstract class AbstractJmsTransactionFunctionalTest extends AbstractJmsFunctionalTestCase
{

    protected UMOTransaction currentTx;

    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        currentTx = null;
    }

    protected void doTearDown() throws Exception
    {
        TransactionCoordination.getInstance().unbindTransaction(TransactionCoordination.getInstance().getTransaction());
        super.doTearDown();
    }

    public void testSendNotTransacted() throws Exception
    {
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        final int countDownInitialCount = 2;
        final CountDownLatch countDown = new CountDownLatch(countDownInitialCount);

        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object Component)
            {
                callbackCalled = true;
                assertNull(context.getCurrentTransaction());
                countDown.countDown();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_NONE, callback);
        addResultListener(getOutDest().getAddress(), countDown);
        managementContext.start();
        afterInitialise();
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        countDown.tryLock(LOCK_WAIT, TimeUnit.MILLISECONDS);
        assertTrue("Only " + (countDownInitialCount - countDown.getCount())
                + " of " + countDownInitialCount
                + " checkpoints hit", countDown.tryLock());

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);
        assertNull(currentTx);
    }

    public void testSendTransactedAlways() throws Exception
    {
        final int countDownInitialCount = 2;
        final CountDownLatch countDown = new CountDownLatch(countDownInitialCount);

        // setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                countDown.countDown();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, callback);

        // Start the server
        managementContext.start();
        addResultListener(getOutDest().getAddress(), countDown);

        // Send a test message first so that it is there when the component is
        // started
        send(DEFAULT_MESSAGE, false, getAcknowledgementMode());

        countDown.tryLock(LOCK_WAIT, TimeUnit.MILLISECONDS);
        assertTrue("Only " + (countDownInitialCount - countDown.getCount())
                + " of " + countDownInitialCount
                + " checkpoints hit", countDown.tryLock());

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);
        assertTrue(currentTx.isBegun());
        // TODO for some reason, it takes a while for committed flag on the tx
        // to update
        Thread.sleep(1000);
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
        final int countDownInitialCount = 2;
        final CountDownLatch countDown = new CountDownLatch(countDownInitialCount);

        // setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                if (transactionAvailable) {
                    assertNotNull(currentTx);
                    assertTrue(currentTx.isBegun());
                } else {
                    assertNull(currentTx);
                }
                countDown.countDown();
            }
        };

        initialiseComponent(descriptor, (transactionAvailable ? UMOTransactionConfig.ACTION_ALWAYS_BEGIN
                : UMOTransactionConfig.ACTION_NONE), callback);

        // Start the server
        managementContext.start();
        addResultListener(getOutDest().getAddress(), countDown);

        // Send a test message firstso that it is there when the component is
        // started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        countDown.tryLock(LOCK_WAIT, TimeUnit.MILLISECONDS);
        assertTrue("Only " + (countDownInitialCount - countDown.getCount())
                + " of " + countDownInitialCount
                + " checkpoints hit", countDown.tryLock(LOCK_WAIT, TimeUnit.MILLISECONDS));

        assertNotNull(currentMsg);
        assertTrue(currentMsg instanceof TextMessage);
        assertEquals(DEFAULT_MESSAGE + " Received", ((TextMessage) currentMsg).getText());
        assertTrue(callbackCalled);

        if (transactionAvailable) {
            assertNotNull(currentTx);
            assertTrue(currentTx.isBegun());
            // TODO for some reason, it takes a while for committed flag on the
            // tx to update
            Thread.sleep(300);
            assertTrue(currentTx.isCommitted());
        } else {
            assertNull(currentTx);
        }
    }

    public void testSendTransactedRollback() throws Exception
    {
        final int countDownInitialCount = 2;
        final CountDownLatch countDown = new CountDownLatch(countDownInitialCount);

        // This exception strategy will be invoked when a message is redelivered
        // after a rollback

        // setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                System.out.println("@@@@ Rolling back transaction @@@@");
                currentTx.setRollbackOnly();
                countDown.countDown();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, callback);
        UMOManager manager = managementContext;
        addResultListener(getOutDest().getAddress(), countDown);

        UMOConnector umoCnn = managementContext.getRegistry().lookupConnector(CONNECTOR_NAME);
        // Set the test Exception strategy
        umoCnn.setExceptionListener(new RollbackExceptionListener(countDown));

        // Start the server
       managementContext.start();

        // Send a test message firstso that it is there when the component is
        // started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        afterInitialise();
        countDown.tryLock(LOCK_WAIT, TimeUnit.MILLISECONDS);
        assertTrue("Only " + (countDownInitialCount - countDown.getCount())
                + " of " + countDownInitialCount
                + " checkpoints hit", countDown.tryLock());

        // Sleep a while to allow transaction to be rolled back
        afterInitialise();

        assertNull(currentMsg);
        assertTrue(callbackCalled);
        assertTrue(currentTx.isRolledBack());

        // Make sure the message isn't on the queue
        assertNull(receive(getInDest().getAddress(), 2000));
    }

    public void testCleanup() throws Exception
    {
        assertNull("There should be no transaction associated with this thread",
                   TransactionCoordination.getInstance().getTransaction());
    }

    public UMOComponent initialiseComponent(UMODescriptor descriptor, byte txBeginAction, EventCallback callback)
            throws Exception
    {
        JMSMessageToObject inTrans = new JMSMessageToObject();
        ObjectToJMSMessage outTrans = new ObjectToJMSMessage();

        UMOEndpoint endpoint = new MuleEndpoint("testIn",
                                                getInDest(),
                                                connector,
                                                inTrans,
                                                UMOEndpoint.ENDPOINT_TYPE_RECEIVER,
                                                0,
                                                null, null);

        UMOTransactionConfig txConfig = new MuleTransactionConfig();
        txConfig.setFactory(getTransactionFactory());
        txConfig.setAction(txBeginAction);

        UMOEndpoint outProvider = new MuleEndpoint("testOut",
                                                   getOutDest(),
                                                   connector,
                                                   outTrans,
                                                   UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                   0,
                                                   null, null);

        endpoint.setTransactionConfig(txConfig);

        descriptor.setOutboundEndpoint(outProvider);
        descriptor.setInboundEndpoint(endpoint);
        HashMap props = new HashMap();
        props.put("eventCallback", callback);
        descriptor.setProperties(props);
        UMOComponent component = managementContext.getModel().registerComponent(descriptor);
        // managementContext.registerConnector(connector);
        return component;
    }

    public static UMODescriptor getDescriptor(String name, String implementation)
    {
        UMODescriptor descriptor = new MuleDescriptor();
        descriptor.setExceptionListener(new DefaultExceptionStrategy());
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
        try {
            if (cnn instanceof QueueConnection) {
                return new MuleEndpointURI(DEFAULT_IN_QUEUE);
            } else {
                return new MuleEndpointURI(DEFAULT_IN_TOPIC);
            }
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getOutDest()
    {
        try {
            if (cnn instanceof QueueConnection) {
                return new MuleEndpointURI(DEFAULT_OUT_QUEUE);
            } else {
                return new MuleEndpointURI(DEFAULT_OUT_TOPIC);
            }
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected UMOEndpointURI getDLDest()
    {
        try {
            if (cnn instanceof QueueConnection) {
                return new MuleEndpointURI(DEFAULT_DL_QUEUE);
            } else {
                return new MuleEndpointURI(DEFAULT_DL_TOPIC);
            }
        } catch (MalformedEndpointException e) {
            fail(e.getMessage());
            return null;
        }
    }

    protected void send(String payload, boolean transacted, int ack) throws JMSException
    {
        if (cnn instanceof QueueConnection) {
            JmsTestUtils.queueSend((QueueConnection) cnn, getInDest().getAddress(), payload, transacted, ack, null);
        } else {
            JmsTestUtils.topicPublish((TopicConnection) cnn, getInDest().getAddress(), payload, transacted, ack);
        }
    }

    protected int getAcknowledgementMode()
    {
        return Session.AUTO_ACKNOWLEDGE;
    }

    protected void addResultListener(String dest, final CountDownLatch countDown) throws JMSException
    {
        MessageConsumer mc;
        // check replyTo
        if (useTopics()) {
            mc = JmsTestUtils.getTopicSubscriber((TopicConnection) cnn, dest);
        } else {
            mc = JmsTestUtils.getQueueReceiver((QueueConnection) cnn, dest);
        }
        mc.setMessageListener(new MessageListener() {
            public void onMessage(Message message)
            {
                currentMsg = message;
                if (countDown != null)
                    countDown.countDown();
            }
        });
    }

    public abstract UMOTransactionFactory getTransactionFactory();

    public abstract UMOConnector createConnector() throws Exception;

    private class RollbackExceptionListener extends DefaultExceptionStrategy
    {
        private CountDownLatch countDown;

        public RollbackExceptionListener(CountDownLatch countDown)
        {
            this.countDown = countDown;
        }

        public RollbackExceptionListener(CountDownLatch countDown, UMOEndpointURI deadLetter) throws UMOException
        {
            this.countDown = countDown;
            UMOEndpoint ep = MuleEndpoint.createEndpointFromUri(deadLetter, UMOEndpoint.ENDPOINT_TYPE_SENDER);
            // lets include dispatch to the deadLetter queue in the sme tx.
            ep.setTransactionConfig(new MuleTransactionConfig());
            ep.getTransactionConfig().setAction(UMOTransactionConfig.ACTION_JOIN_IF_POSSIBLE);
            super.addEndpoint(ep);
        }

        public void handleMessagingException(UMOMessage message, Throwable t)
        {
            System.out.println("@@@@ ExceptionHandler Called @@@@");
            if (t instanceof MessageRedeliveredException) {
                countDown.countDown();
                try {
                    // MessageRedeliveredException mre =
                    // (MessageRedeliveredException)t;
                    Message msg = (Message) message.getPayload();

                    assertNotNull(msg);
                    assertTrue(msg.getJMSRedelivered());
                    assertTrue(msg instanceof TextMessage);
                    // No need to commit transaction as the Tx template will
                    // auto
                    // matically commit by default
                    super.handleMessagingException(message, t);
                } catch (Exception e) {
                    fail(e.getMessage());
                }
            } else {
                t.printStackTrace();
                fail(t.getMessage());
            }
            super.handleMessagingException(message, t);
        }

        public void handleException(Throwable t)
        {

        }
    }

    public void testTransactedRedeliveryToDLDestination() throws Exception
    {
        // there are 2 check points for each message delivered, so
        // the message will be delivered twice before this countdown will release
        final int countDownInitialCount = 4;
        final CountDownLatch countDown = new CountDownLatch(countDownInitialCount);

        // setup the component and start Mule
        UMODescriptor descriptor = getDescriptor("testComponent", FunctionalTestComponent.class.getName());

        EventCallback callback = new EventCallback() {
            public void eventReceived(UMOEventContext context, Object Component) throws Exception
            {
                callbackCalled = true;
                currentTx = context.getCurrentTransaction();
                assertNotNull(currentTx);
                assertTrue(currentTx.isBegun());
                System.out.println("@@@@ Rolling back transaction @@@@");
                currentTx.setRollbackOnly();
                countDown.countDown();
            }
        };

        initialiseComponent(descriptor, UMOTransactionConfig.ACTION_ALWAYS_BEGIN, callback);
        UMOManager manager = managementContext;
        addResultListener(getDLDest().getAddress(), countDown);

        JmsConnector umoCnn = (JmsConnector) managementContext.getRegistry().lookupConnector(CONNECTOR_NAME);

        // After redelivery retry the message and then fail
        umoCnn.setMaxRedelivery(1);

        // Set the test Exception strategy
        umoCnn.setExceptionListener(new RollbackExceptionListener(countDown, getDLDest()));

        // Start the server
       managementContext.start();

        // Send a test message firstso that it is there when the component is
        // started
        send(DEFAULT_MESSAGE, false, Session.AUTO_ACKNOWLEDGE);

        afterInitialise();
        countDown.tryLock(LOCK_WAIT, TimeUnit.MILLISECONDS);
        assertTrue("Only " + (countDownInitialCount - countDown.getCount())
                + " of " + countDownInitialCount
                + " checkpoints hit", countDown.tryLock());

        assertNotNull(currentMsg);
        System.out.println(currentMsg);
        String dest = currentMsg.getStringProperty(MuleProperties.MULE_ENDPOINT_PROPERTY);
        assertNotNull(dest);
        assertEquals(getDLDest().getUri().toString(), dest);
        assertTrue(callbackCalled);

        // Make sure the message isn't on the queue
        assertNull(receive(getInDest().getAddress(), 2000));
    }
}
