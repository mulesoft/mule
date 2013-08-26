/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.mortbay.thread.QueuedThreadPool;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.jetty.JettyHttpConnector;

import java.util.Set;

import static org.junit.Assert.assertEquals;

/**
 * Functional tests that Jetty acceptor threads may be changed (from default of 1).
 * Implementors should define a Mule config containing a single Jetty connector
 * otherwise the number of acceptor threads will be cumulative across all connectors.
 */
public abstract class AbstractJettyAcceptorFunctionalTestCase extends FunctionalTestCase {

    enum Protocol {
        http, https
    }

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Override
    protected String getConfigResources() {
        return null;
    }

    void assertAcceptors(final String connectorName, final String flowName, final int acceptors, final Protocol protocol) throws Exception {
        assertGlobalConnector(connectorName, acceptors);
        assertFlowConnector(flowName, acceptors);
        assertThreads(acceptors);
        assertRequest(protocol);
    }

    void assertFlowConnector(String flowName, int acceptors) {
        // verify the acceptor config is passed down into the connector used by the endpoint
        final Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        Assert.assertNotNull(flow);
        final InboundEndpoint endpoint = (InboundEndpoint) flow.getMessageSource();
        Assert.assertNotNull(endpoint);
        final Connector conn = endpoint.getConnector();
        Assert.assertNotNull(conn);
        Assert.assertThat(conn, CoreMatchers.instanceOf(JettyHttpConnector.class));
        final JettyHttpConnector cnn = (JettyHttpConnector) conn;
        Assert.assertEquals(acceptors, cnn.getAcceptors());
    }

    void assertGlobalConnector(final String connectorName, final int acceptors) {
        // verify the acceptor config is passed down into the application defined connector
        final Connector conn = muleContext.getRegistry().lookupConnector(connectorName);
        Assert.assertNotNull(conn);
        Assert.assertThat(conn, CoreMatchers.instanceOf(JettyHttpConnector.class));
        final JettyHttpConnector cnn = (JettyHttpConnector) conn;
        Assert.assertEquals(acceptors, cnn.getAcceptors());
    }

    void assertThreads(final int acceptors) {
        final Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
        int actual = 0;
        for (final Thread th : threadSet) {
            if (th.getClass().equals(QueuedThreadPool.PoolThread.class)
                    && th.getName().contains("Acceptor")) {
                actual = actual + 1;
            }
        }
        Assert.assertEquals(acceptors, actual);
    }

    void assertRequest(final Protocol protocol) throws Exception {
        final MuleClient client = muleContext.getClient();
        final MuleMessage message = client.send(String.format("%s://localhost:%s", protocol, port1.getNumber()), TEST_MESSAGE, null);
        assertEquals("200", message.getInboundProperty("http.status"));
        assertEquals(TEST_MESSAGE + " received", message.getPayloadAsString());
    }
}
