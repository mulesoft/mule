/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.functional.requester;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import org.mule.construct.Flow;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.io.EndPoint;
import org.eclipse.jetty.server.Request;
import org.junit.Test;

public class HttpRequestConnectionsPersistenceTestCase extends AbstractHttpRequestTestCase
{
    private static final int GRIZZLY_IDLE_CHECK_TIMEOUT_MILLIS = 6000;
    private static final int POLL_DELAY_MILLIS = 200;
    private int remotePort;

    @Override
    protected String getConfigFile()
    {
        return "http-request-connections-persistence-config.xml";
    }

    @Test
    public void persistentConnections() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("persistent");
        flow.process(getTestEvent(TEST_MESSAGE));
        ensureConnectionIsOpen();

        new PollingProber(GRIZZLY_IDLE_CHECK_TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitProbe()
        {
            @Override
            public boolean test() throws Exception
            {
                return isConnectionClosed();
            }

            @Override
            public String describeFailure()
            {
                return "Connection should be closed.";
            }
        });
    }

    @Test
    public void nonPersistentConnections() throws Exception
    {
        Flow flow = (Flow) getFlowConstruct("nonPersistent");
        flow.process(getTestEvent(TEST_MESSAGE));
        assertThat(isConnectionClosed(), is(true));
    }

    private void ensureConnectionIsOpen()
    {
        EndPoint endPoint = getConnectedEndPoint();

        assertThat(endPoint, is(notNullValue()));

        assertThat(endPoint.getLocalAddress().getPort(), is(httpPort.getNumber()));
        assertThat(endPoint.getRemoteAddress().getPort(), is(remotePort));
    }

    private boolean isConnectionClosed()
    {
        return getConnectedEndPoint() == null;
    }

    private EndPoint getConnectedEndPoint()
    {
        assertThat(server.getConnectors().length, is(1));

        Collection<EndPoint> connectedEndpoints = server.getConnectors()[0].getConnectedEndPoints();

        if (!connectedEndpoints.isEmpty())
        {
            return connectedEndpoints.iterator().next();
        }
        return null;
    }

    @Override
    protected void handleRequest(Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException
    {
        super.handleRequest(baseRequest, request, response);
        remotePort = request.getRemotePort();
    }


}


