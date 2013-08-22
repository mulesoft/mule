/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.servlet.jetty.functional;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.transport.servlet.jetty.JettyHttpConnector;

/**
 * Functional tests that Jetty acceptor threads may be changed (from default of 1)
 */
public abstract class AbstractJettyAcceptorFunctionalTestCase extends FunctionalTestCase {

    @Rule
    public DynamicPort port1 = new DynamicPort("port1");

    @Rule
    public DynamicPort port2 = new DynamicPort("port2");

    @Override
    protected String getConfigResources() {
        return null;
    }

    void assertAcceptors(final String connectorName, final String flowName, final int acceptors) {
        // verify the acceptor config is passed down into the application defined connector
        Connector conn = muleContext.getRegistry().lookupConnector(connectorName);
        Assert.assertNotNull(conn);
        Assert.assertThat(conn, CoreMatchers.instanceOf(JettyHttpConnector.class));
        JettyHttpConnector cnn = (JettyHttpConnector) conn;
        Assert.assertEquals(acceptors, cnn.getAcceptors());

        // verify the acceptor config is passed down into the connector used by the endpoint
        final Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct(flowName);
        Assert.assertNotNull(flow);
        final InboundEndpoint endpoint = (InboundEndpoint) flow.getMessageSource();
        Assert.assertNotNull(endpoint);
        conn = endpoint.getConnector();
        Assert.assertNotNull(conn);
        Assert.assertThat(conn, CoreMatchers.instanceOf(JettyHttpConnector.class));
        cnn = (JettyHttpConnector) conn;
        Assert.assertEquals(acceptors, cnn.getAcceptors());
    }
}
