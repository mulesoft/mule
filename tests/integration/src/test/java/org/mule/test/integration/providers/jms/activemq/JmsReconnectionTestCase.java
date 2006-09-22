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

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;
import org.activemq.ActiveMQConnectionFactory;
import org.activemq.broker.impl.BrokerContainerFactoryImpl;
import org.activemq.store.vm.VMPersistenceAdapter;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.config.builders.QuickConfigurationBuilder;
import org.mule.extras.client.MuleClient;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.internal.notifications.ConnectionNotification;
import org.mule.impl.internal.notifications.ConnectionNotificationListener;
import org.mule.impl.model.seda.SedaModel;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.providers.jms.JmsConnector;
import org.mule.providers.jms.JmsConstants;
import org.mule.tck.functional.FunctionalTestComponent;
import org.mule.tck.functional.FunctionalTestNotificationListener;
import org.mule.test.integration.ServerTools;
import org.mule.test.integration.providers.jms.AbstractJmsFunctionalTestCase;
import org.mule.umo.UMOException;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOServerNotification;
import org.mule.util.concurrent.Latch;

import javax.jms.ConnectionFactory;

/**
 * This test needs the path to an activemq distribution.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 */
public class JmsReconnectionTestCase extends AbstractJmsFunctionalTestCase implements ConnectionNotificationListener {

    private JmsConnector connector;
    private BlockingQueue events = new LinkedBlockingQueue();

    private long TIME_OUT = 10000L;
    public static final String BROKER_URL = "tcp://localhost:56312";

    protected ActiveMQConnectionFactory factory = null;
    protected Latch eventLatch1 = new Latch();
    protected Latch eventLatch2 = new Latch();

    public ConnectionFactory getConnectionFactory() throws Exception
    {
        if(factory==null) {
            factory = new ActiveMQConnectionFactory();
            factory.setBrokerContainerFactory(new BrokerContainerFactoryImpl(new VMPersistenceAdapter()));
            factory.setBrokerURL(BROKER_URL);
            factory.start();
        }
        return factory;
    }

    protected void doSetUp() throws Exception {
        // By default the JmsTestUtils use the openjms config, though you can
        // pass
        // in other configs using the property below

        // Make sure we are running synchronously
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration()
                .getPoolingProfile()
                .setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        MuleManager.getInstance().setModel(new SedaModel());
        callbackCalled = false;
        MuleManager.getInstance().registerConnector(createConnector());
        currentMsg = null;
        eventLatch1 = new Latch();
        eventLatch2 = new Latch();

        QuickConfigurationBuilder builder = new QuickConfigurationBuilder();
        builder.registerComponent(FunctionalTestComponent.class.getName(),
                "testJmsReconnection", getReceiverEndpoint("jms://reconnect.queue"), null, null);
    }

    /**
     * Use this method to do any validation such as check for an installation of a required server
     * If the current environment does not have the preReqs of the test return false and the test will
     * be skipped.
     *
     */
    protected String checkPreReqs() {
        if(System.getProperty(ServerTools.ACTIVEMQ_HOME, null)!=null) {
            return null;
        } else {
            return "You must set the " + ServerTools.ACTIVEMQ_HOME + " system property to the root path of an ActiveMq distribution (v3.0 and greater) before running these tests";
        }
    }

    protected void doTearDown() throws Exception {
        ServerTools.killActiveMq();
    }

    protected UMOEndpoint getReceiverEndpoint(String URI) throws UMOException {
        return new MuleEndpoint(URI, true);
    }

    public JmsConnector createConnector() throws Exception {
        connector = new JmsConnector();
        connector.setSpecification(JmsConstants.JMS_SPECIFICATION_11);
        connector.setConnectionFactory(getConnectionFactory());
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        SimpleRetryConnectionStrategy strategy = new SimpleRetryConnectionStrategy();
        strategy.setRetryCount(5);
        strategy.setFrequency(3000);
        strategy.setDoThreading(true);
        connector.setConnectionStrategy(strategy);

        return connector;
    }

    public void testReconnection() throws Exception {

        if(!isPrereqsMet("org.mule.test.integration.providers.jms.activemq.JmsReconnectionTestCase.testReconnection()")) {
            return;
        }

        MuleManager.getInstance().start();
        MuleManager.getInstance().registerListener(this);

        // Start time
        long t0, t1;
        // Check that connection fails
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionNotification event = (ConnectionNotification) events.poll(TIME_OUT, TimeUnit.MILLISECONDS);
            if (event != null && event.getAction() == ConnectionNotification.CONNECTION_FAILED) {
                break;
            } else {
                fail("no notification event was received: " + event);
            }
            t1 = System.currentTimeMillis() - t0;
            if (t1 > TIME_OUT) {
                fail("No connection attempt");
            }
        }

        // Launch activemq
        ServerTools.launchActiveMq(BROKER_URL);
        // Check that connection succeed
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionNotification event = (ConnectionNotification)  events.poll(TIME_OUT, TimeUnit.MILLISECONDS);
            if (event.getAction() == ConnectionNotification.CONNECTION_CONNECTED) {
                break;
            }
            t1 = System.currentTimeMillis() - t0;
            if (t1 > TIME_OUT) {
                fail("Connection should have succeeded");
            }
        }

        Thread.sleep(3000);
        MuleClient client = new MuleClient();

        MuleManager.getInstance().registerListener(new FunctionalTestNotificationListener() {
            public void onNotification(UMOServerNotification notification) {
                if(notification.getSource().equals("test1")) {
                    eventLatch1.countDown();
                } else if(notification.getSource().equals("test2")) {
                    eventLatch2.countDown();
                }

            }
        });
        client.sendNoReceive("jms://reconnect.queue", "test1", null);
        //we should be able to do a sync call here and get a response message back
        //but there is a bug in ActiveMq that causes a null pointer
        //assertNotNull(m);
        //assertEquals("Received: test", m.getPayloadAsString());
        assertTrue("1st Event should have been received", eventLatch1.await(15000L, TimeUnit.MILLISECONDS));

        // Kill activemq
        ServerTools.killActiveMq();
        // Check that the connection is lost
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionNotification event = (ConnectionNotification)  events.poll(TIME_OUT, TimeUnit.MILLISECONDS);
            assertNotNull("Disconnect event should have been received", event);
            if (event.getAction() == ConnectionNotification.CONNECTION_DISCONNECTED) {
                break;
            }
            t1 = System.currentTimeMillis() - t0;
            if (t1 > TIME_OUT) {
                fail("Connection should have been lost");
            }
        }
        // Restart activemq
        ServerTools.launchActiveMq(BROKER_URL);
        // Check that connection succeed
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionNotification event = (ConnectionNotification) events.poll(TIME_OUT, TimeUnit.MILLISECONDS);
            if (event.getAction() == ConnectionNotification.CONNECTION_CONNECTED) {
                break;
            }
            t1 = System.currentTimeMillis() - t0;
            if (t1 > TIME_OUT) {
                fail("Connection should have succeeded");
            }
        }

        //Lets send another test message to esure everything is back up
        client.sendNoReceive("jms://reconnect.queue", "test2", null);
        assertTrue("2nd Event should have been received", eventLatch2.await(15000L, TimeUnit.MILLISECONDS));

    }


    public void onNotification(UMOServerNotification notification) {
        try {
            events.put(notification);
        } catch (InterruptedException e) {
            throw new RuntimeException("Caught unexpected exception:", e);
        }
    }
}
