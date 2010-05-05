/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.jms;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.tck.AbstractMuleTestCase;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;

import org.mockito.Matchers;

public class JmsConnectorTest extends AbstractMuleTestCase
{
    private static final String CLIENT_ID1 = "client1";
    private static final String CLIENT_ID2 = "client2";

    /**
     * Tests that client ID is set on the connection if it is originally null.
     */
    public void testSetClientIDInConnectorForFirstTime() throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(connection.getClientID()).thenReturn(null);

        JmsSupport jmsSupport = mock(JmsSupport.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory> any())).thenReturn(connection);

        JmsConnector connector = new JmsConnector(muleContext);
        connector.setClientId(CLIENT_ID1);
        connector.setJmsSupport(jmsSupport);

        Connection createdConnection = connector.createConnection();

        assertEquals(connection, createdConnection);
        verify(connection, times(1)).setClientID(Matchers.anyString());
        verify(connection, times(1)).setClientID(CLIENT_ID1);
    }

    /**
     * Tests that client ID is set on the connection if it has a different client ID.
     */
    public void testSetClientIDInConnectorForSecondTime() throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(connection.getClientID()).thenReturn(CLIENT_ID1);

        JmsSupport jmsSupport = mock(JmsSupport.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory> any())).thenReturn(connection);

        JmsConnector connector = new JmsConnector(muleContext);
        connector.setClientId(CLIENT_ID2);
        connector.setJmsSupport(jmsSupport);

        Connection createdConnection = connector.createConnection();

        assertEquals(connection, createdConnection);
        verify(connection, times(1)).setClientID(Matchers.anyString());
        verify(connection, times(1)).setClientID(CLIENT_ID2);
    }

    /**
     * Tests that client ID is not set on the connection if it has the same client
     * ID.
     */
    public void testSetClientIDInConnectionForFirstTime() throws Exception
    {
        final Connection connection = mock(Connection.class);
        when(connection.getClientID()).thenReturn(CLIENT_ID1);

        JmsSupport jmsSupport = mock(JmsSupport.class);
        when(jmsSupport.createConnection(Matchers.<ConnectionFactory> any())).thenReturn(connection);
        
        JmsConnector connector = new JmsConnector(muleContext);
        connector.setClientId(CLIENT_ID1);
        connector.setJmsSupport(jmsSupport);

        Connection createdConnection = connector.createConnection();

        assertEquals(connection, createdConnection);
        verify(connection, times(0)).setClientID(Matchers.anyString());
    }
}
