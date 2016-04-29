/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.broker.TransportConnector;
import org.junit.Rule;

/**
 * Base test class for tests that require an Active MQ broker that is not embedded in the Mule application.
 */
public class AbstractBrokerFunctionalTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort port = new DynamicPort("port");

    protected BrokerService broker;
    protected TransportConnector transportConnector;
    protected String url;


    @Override
    protected void doSetUpBeforeMuleContextCreation() throws Exception
    {
        url = "tcp://localhost:" + this.port.getValue();
        startBroker();
    }

    @Override
    protected void doTearDownAfterMuleContextDispose() throws Exception
    {
        stopBroker();
    }

    protected void startBroker() throws Exception
    {
        broker = new BrokerService();
        broker.setUseJmx(false);
        broker.setPersistent(false);

        transportConnector = broker.addConnector(this.url);

        broker.start(true);
        broker.waitUntilStarted();
    }

    protected void stopBroker() throws Exception
    {
        broker.stop();
        broker.waitUntilStopped();
    }

    protected int getConnectionsCount()
    {
        return transportConnector.getConnections().size();
    }

}
