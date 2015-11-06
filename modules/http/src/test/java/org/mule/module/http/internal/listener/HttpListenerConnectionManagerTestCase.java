/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.http.internal.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.MuleRuntimeException;
import org.mule.api.context.WorkManagerSource;
import org.mule.module.http.api.HttpConstants;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.transport.tcp.TcpServerSocketProperties;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Answers;

public class HttpListenerConnectionManagerTestCase extends AbstractMuleTestCase
{

    private static final String SPECIFIC_IP = "172.24.24.1";
    public static final int PORT = 5555;
    public static final int CONNECTION_IDLE_TIMEOUT = 1000;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void initializationFailsWhenHostIsRepeated() throws Exception
    {
        testInitialization(SPECIFIC_IP, SPECIFIC_IP);
    }

    @Test
    public void initializationFailsWhenSpecificHostIsOverlapping() throws Exception
    {
        testInitialization(HttpConstants.ALL_INTERFACES_IP, SPECIFIC_IP);
    }

    @Test
    public void initializationFailsWhenAllInterfacesIsOverlapping() throws Exception
    {
        testInitialization(SPECIFIC_IP, HttpConstants.ALL_INTERFACES_IP);
    }

    private void testInitialization(String firstIp, String secondIp) throws MuleException
    {
        final HttpListenerConnectionManager connectionManager = new HttpListenerConnectionManager();
        final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
        connectionManager.setMuleContext(mockMuleContext);
        WorkManagerSource mockWorkManagerSource = mock(WorkManagerSource.class);
        when((Object) (mockMuleContext.getRegistry().lookupObject(TcpServerSocketProperties.class))).thenReturn(mock(TcpServerSocketProperties.class));

        connectionManager.initialise();
        connectionManager.createServer(new ServerAddress(firstIp, PORT), mockWorkManagerSource, false, CONNECTION_IDLE_TIMEOUT);
        expectedException.expect(MuleRuntimeException.class);
        expectedException.expectMessage(String.format(HttpListenerConnectionManager.SERVER_ALREADY_EXISTS_FORMAT, PORT, secondIp));

        try
        {
            connectionManager.createServer(new ServerAddress(secondIp, PORT), mockWorkManagerSource, false, CONNECTION_IDLE_TIMEOUT);
        }
        finally
        {
            connectionManager.dispose();
        }
    }
}
