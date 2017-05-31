/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.ftp;

import static java.lang.Integer.parseInt;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.transport.ftp.FtpConnector.ASYNCHRONOUS_RECONNECTION_ERROR_MESSAGE;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mule.api.construct.FlowConstruct;
import org.mule.api.endpoint.EndpointURI;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

public class FtpReconnectionStrategyTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort ftpPort = new DynamicPort("port");

    private final FlowConstruct flowConstruct = mock (FlowConstruct.class);
    private final InboundEndpoint inboundEndpoint = mock(InboundEndpoint.class, RETURNS_DEEP_STUBS);
    private  FtpMessageReceiver ftpMessageReceiver = null;
    private Connector connector = null;

    @Override
    protected String getConfigFile()
    {
        return "ftp-asynchronous-reconnection-config.xml";
    }

    @Before
    public void setUp() throws Exception
    {
        connector = muleContext.getRegistry().lookupConnector("FTP");
        EndpointURI endpointUri = mock(EndpointURI.class, RETURNS_DEEP_STUBS);
        when(inboundEndpoint.getConnector()).thenReturn(connector);
        when(inboundEndpoint.getRetryPolicyTemplate()).thenReturn(null);
        when(inboundEndpoint.getEndpointURI()).thenReturn(endpointUri);
        when(inboundEndpoint.getMuleContext()).thenReturn(muleContext);
        when(endpointUri.getHost()).thenReturn("localhost");
        when(endpointUri.getPort()).thenReturn(parseInt(ftpPort.getValue()));
        when(endpointUri.getUser()).thenReturn("user");
        when(endpointUri.getPassword()).thenReturn("password");
        ftpMessageReceiver = new FtpMessageReceiver(connector, flowConstruct, inboundEndpoint, 1000);
        ftpMessageReceiver.initialise();
    }

    @Test
    public void testAsynchronousReconnectionStrategyInReceiver()
    {
        try
        {
            ftpMessageReceiver.listFiles();
            fail("As asynchronous reconnection strategy is not supported in FTP Connector, an exception must be triggered");
        }
        catch (Exception e)
        {
            assertThat(e.getMessage(), is(ASYNCHRONOUS_RECONNECTION_ERROR_MESSAGE));
        }
    }

    @Test
    public void testAsynchronousReconnectionStrategyInDispatcher()
    {
        try
        {
            runFlow("testDispatcher");
            fail("As asynchronous reconnection strategy is not supported in FTP Connector, an exception must be triggered");
        }
        catch (Exception e)
        {
            assertThat(e.getCause().getMessage(), is(ASYNCHRONOUS_RECONNECTION_ERROR_MESSAGE));
        }
    }

}

