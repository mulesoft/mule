/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.jms.activemq;

import EDU.oswego.cs.dl.util.concurrent.LinkedQueue;
import org.mule.MuleManager;
import org.mule.config.PoolingProfile;
import org.mule.impl.MuleDescriptor;
import org.mule.impl.MuleModel;
import org.mule.impl.endpoint.MuleEndpoint;
import org.mule.impl.endpoint.MuleEndpointURI;
import org.mule.impl.internal.events.ConnectionEvent;
import org.mule.impl.internal.events.ConnectionEventListener;
import org.mule.providers.SimpleRetryConnectionStrategy;
import org.mule.providers.jms.JmsConnector;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.test.integration.ServerTools;
import org.mule.test.integration.providers.jms.AbstractJmsFunctionalTestCase;
import org.mule.test.integration.providers.jms.tools.JmsTestUtils;
import org.mule.umo.UMOComponent;
import org.mule.umo.endpoint.UMOEndpoint;
import org.mule.umo.manager.UMOServerEvent;
import org.mule.umo.provider.UMOConnector;

import javax.jms.Connection;
import java.util.HashMap;
import java.util.Properties;

/**
 * 
 * This test needs the path to an activemq distribution.
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 *
 */
public class JmsConnectionTestCase extends AbstractJmsFunctionalTestCase implements ConnectionEventListener {

    private JmsConnector connector;
    private LinkedQueue events = new LinkedQueue();

    private long TIME_OUT = 10000L;
    public static final String BROKER_URL = "tcp://localhost:56312";

    protected void doSetUp() throws Exception
    {
        // By default the JmsTestUtils use the openjms config, though you can
        // pass
        // in other configs using the property below

        // Make sure we are running synchronously
        MuleManager.getConfiguration().setSynchronous(true);
        MuleManager.getConfiguration()
                   .getPoolingProfile()
                   .setInitialisationPolicy(PoolingProfile.POOL_INITIALISE_ONE_COMPONENT);

        MuleManager.getInstance().setModel(new MuleModel());
        callbackCalled = false;
        MuleManager.getInstance().registerConnector(createConnector());
        currentMsg = null;
        eventCount = 0;
    }

    protected void doTearDown() throws Exception
    {
        ServerTools.killActiveMq();
    }

    public UMOConnector createConnector() throws Exception
    {
        connector = new JmsConnector();
        connector.setSpecification(JmsConnector.JMS_SPECIFICATION_11);
        Properties props = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);
        connector.setConnectionFactoryJndiName("JmsQueueConnectionFactory");
        Properties factoryProps = new Properties();
        factoryProps.setProperty("brokerURL", BROKER_URL);
        connector.setJndiProviderProperties(props);
        connector.setConnectionFactoryProperties(factoryProps);
        connector.setName(CONNECTOR_NAME);
        connector.getDispatcherThreadingProfile().setDoThreading(false);

        SimpleRetryConnectionStrategy strategy = new SimpleRetryConnectionStrategy();
        strategy.setRetryCount(10);
        strategy.setFrequency(5000);
        strategy.setDoThreading(true);
        connector.setConnectionStrategy(strategy);

        return connector;
    }

    public Connection getConnection() throws Exception
    {
        // default to ActiveMq for Jms 1.1 support
        Properties p = JmsTestUtils.getJmsProperties(JmsTestUtils.ACTIVE_MQ_JMS_PROPERTIES);
        p.setProperty("brokerURL", BROKER_URL);
        return JmsTestUtils.getQueueConnection(p);
    }

    public void testReconnection() throws Exception {

        MuleDescriptor d = getTestDescriptor("anOrange", Orange.class.getName());

        UMOComponent component = MuleManager.getInstance().getModel().registerComponent(d);
        UMOEndpoint endpoint = new MuleEndpoint("test",
                                                new MuleEndpointURI("jms://my.queue"),
                                                connector,
                                                null,
                                                UMOEndpoint.ENDPOINT_TYPE_SENDER,
                                                0,
                                                new HashMap());
        MuleManager.getInstance().start();
        MuleManager.getInstance().registerListener(this);
        connector.registerListener(component, endpoint);

        // Start time
        long t0, t1;
        // Check that connection fails
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionEvent event = (ConnectionEvent) events.take();
            if (event.getAction() == ConnectionEvent.CONNECTION_FAILED) {
                break;
            }
            t1 = System.currentTimeMillis() -t0;
            if (t1 > TIME_OUT) {
                fail("No connection attempt");
            }
        }

        // Launch activemq
        ServerTools.launchActiveMq(BROKER_URL);
        // Check that connection succeed
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionEvent event = (ConnectionEvent) events.take();
            if (event.getAction() == ConnectionEvent.CONNECTION_CONNECTED) {
                break;
            }
            t1 = System.currentTimeMillis() -t0;
            if (t1 > TIME_OUT) {
                fail("Connection should have succeeded");
            }
        }
        // Kill activemq
        ServerTools.killActiveMq();
        // Check that the connection is lost
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionEvent event = (ConnectionEvent) events.take();
            if (event.getAction() == ConnectionEvent.CONNECTION_DISCONNECTED) {
                break;
            }
            t1 = System.currentTimeMillis() -t0;
            if (t1 > TIME_OUT) {
                fail("Connection should have been lost");
            }
        }
        // Restart activemq
        ServerTools.launchActiveMq(BROKER_URL);
        // Check that connection succeed
        t0 = System.currentTimeMillis();
        while (true) {
            ConnectionEvent event = (ConnectionEvent) events.take();
            if (event.getAction() == ConnectionEvent.CONNECTION_CONNECTED) {
                break;
            }
            t1 = System.currentTimeMillis() -t0;
            if (t1 > TIME_OUT) {
                fail("Connection should have succeeded");
            }
        }
        ServerTools.killActiveMq();

    }



    public void onEvent(UMOServerEvent event) {
        try {
            events.put(event);
        } catch (InterruptedException e) {
        }
    }

}
