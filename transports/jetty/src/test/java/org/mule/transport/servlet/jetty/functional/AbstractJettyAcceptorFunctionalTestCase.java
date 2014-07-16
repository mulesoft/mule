/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty.functional;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.transport.servlet.jetty.JettyHttpConnector;

import java.util.Set;

import org.junit.Rule;

/**
 * Functional tests that Jetty acceptor threads may be changed (from default of 1).
 * Implementors should define a Mule config containing a single Jetty connector
 * otherwise the number of acceptor threads will be cumulative across all connectors.
 */
public abstract class AbstractJettyAcceptorFunctionalTestCase extends FunctionalTestCase
{

    protected enum Protocol
    {
        http, https
    }

    @Rule
    public final DynamicPort port1 = new DynamicPort("port1");

    protected void assertAcceptors(final String connectorName, final String flowName, final int acceptors, final Protocol protocol) throws Exception
    {
        assertGlobalConnector(connectorName, acceptors);
        assertFlowConnector(flowName, acceptors);
        assertThreads(acceptors);
        assertRequest(protocol);
    }

    protected void assertFlowConnector(final String flowName, final int acceptors)
    {
        // verify the acceptor config is passed down into the connector used by the endpoint
        final Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        assertNotNull(flow);
        final InboundEndpoint endpoint = (InboundEndpoint) flow.getMessageSource();
        assertNotNull(endpoint);
        final Connector conn = endpoint.getConnector();
        assertNotNull(conn);
        assertThat(conn, instanceOf(JettyHttpConnector.class));
        final JettyHttpConnector cnn = (JettyHttpConnector) conn;
        assertEquals(acceptors, cnn.getAcceptors());
    }

    protected void assertGlobalConnector(final String connectorName, final int acceptors)
    {
        // verify the acceptor config is passed down into the application defined connector
        final Connector conn = muleContext.getRegistry().lookupConnector(connectorName);
        assertNotNull(conn);
        assertThat(conn, instanceOf(JettyHttpConnector.class));
        final JettyHttpConnector cnn = (JettyHttpConnector) conn;
        assertEquals(acceptors, cnn.getAcceptors());
    }

    protected void assertThreads(final int acceptors)
    {
        PollingProber prober = new PollingProber(5000, 50);

        prober.check(new Probe()
        {
            int actual;

            public boolean isSatisfied()
            {
                final Set<Thread> threadSet = Thread.getAllStackTraces().keySet();
                actual = 0;
                for (final Thread th : threadSet)
                {
                    if (th.getName().contains("qtp") && th.getName().contains("acceptor"))
                    {
                        actual = actual + 1;
                    }
                }

                return actual == acceptors;
            }

            public String describeFailure()
            {
                return String.format("Expected '%s' acceptor threads but there are '%s'", acceptors, actual);
            }
        });
    }

    protected void assertRequest(final Protocol protocol) throws Exception
    {
        final MuleClient client = muleContext.getClient();
        final MuleMessage message = client.send(String.format("%s://localhost:%s", protocol, port1.getNumber()), TEST_MESSAGE, null);
        assertEquals("200", message.getInboundProperty("http.status"));
        assertEquals(TEST_MESSAGE + " received", message.getPayloadAsString());
    }
}
