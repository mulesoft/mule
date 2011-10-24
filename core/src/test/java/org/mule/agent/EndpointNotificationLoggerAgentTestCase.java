/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.agent;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleMessage;
import org.mule.api.context.notification.ServerNotification;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.routing.filter.Filter;
import org.mule.api.transport.Connector;
import org.mule.context.notification.ConnectionNotification;
import org.mule.context.notification.MuleContextNotification;

import java.lang.reflect.Field;

import org.apache.commons.logging.Log;
import org.junit.Before;
import org.junit.Test;

public class EndpointNotificationLoggerAgentTestCase
{

    private EndpointNotificationLoggerAgent agent;
    private OutboundEndpoint outboundEndpoint;
    private Connector connector;
    private ServerNotification serverNotification;
    private Filter filterMock;

    @Before
    public void setUp() throws InitialisationException
    {
        agent = new EndpointNotificationLoggerAgent();
        outboundEndpoint = mock(OutboundEndpoint.class);
        agent.logger = mock(Log.class);
        agent.setEndpoint(outboundEndpoint);
        connector = mock(Connector.class);
        serverNotification = mock(ServerNotification.class);
        filterMock = mock(Filter.class);
        agent.setMuleContext(mock(MuleContext.class));
        agent.doInitialise();
        when(outboundEndpoint.getConnector()).thenReturn(connector);
        when(serverNotification.getSource()).thenReturn(connector);
    }

    @Test(expected = InitialisationException.class)
    public void initializationFailsWhenNoEndpoint() throws Exception
    {
        agent.setEndpoint(null);
        agent.doInitialise();
    }

    @Test
    public void initializationCreatesTheSession() throws Exception
    {
        assertNotNull(getAttribute(agent, "session"));
    }

    @Test
    public void notLoggedBecauseNotificationIsIgnored() throws InitialisationException
    {
        when(serverNotification.getAction()).thenReturn(MuleContextNotification.CONTEXT_STOPPED);
        agent.logEvent(serverNotification);
        verify(serverNotification, times(1)).getAction();
    }

    @Test
    public void logEndpointNotStarted() throws InitialisationException
    {
        when(serverNotification.getAction()).thenReturn(MuleContextNotification.CONTEXT_STARTING);
        when(connector.isStarted()).thenReturn(false);
        agent.logEvent(serverNotification);
        verify(agent.logger, times(1)).warn(anyString());
    }

    @Test
    public void ignoreConnectionFailedWhenSameConnector() throws InitialisationException
    {
        when(serverNotification.getAction()).thenReturn(ConnectionNotification.CONNECTION_FAILED);
        when(connector.isStarted()).thenReturn(true);
        agent.logEvent(serverNotification);
        verify(outboundEndpoint, times(0)).getFilter();
    }

    @Test
    public void ignoreConnectionDisconnectedWhenSameConnector() throws InitialisationException
    {
        when(serverNotification.getAction()).thenReturn(ConnectionNotification.CONNECTION_DISCONNECTED);
        when(connector.isStarted()).thenReturn(true);
        agent.logEvent(serverNotification);
        verify(outboundEndpoint, times(0)).getFilter();
    }

    @Test
    public void eventIsLoggedBecauseMessageNotFiltered() throws Exception
    {
        when(agent.logger.isInfoEnabled()).thenReturn(true);
        when(connector.isStarted()).thenReturn(true);
        when(outboundEndpoint.getFilter()).thenReturn(filterMock);
        when(filterMock.accept(any(MuleMessage.class))).thenReturn(false);
        when(serverNotification.getAction()).thenReturn(ConnectionNotification.CONNECTION_CONNECTED);
        agent.logEvent(serverNotification);
        verify(agent.logger, times(1)).info(anyString());
    }

    /**
     * Retrieves the value of the fieldName attribute of the object class.
     */
    private Object getAttribute(Object object, String fieldName) throws Exception
    {
        Field privateStringMethod = object.getClass().getDeclaredField(fieldName);
        privateStringMethod.setAccessible(true);
        return privateStringMethod.get(object);
    }
}
