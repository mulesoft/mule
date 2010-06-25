/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transport.cxf;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.transport.Connector;
import org.mule.tck.AbstractMuleTestCase;

public class ClientWrapperAegisTestCase extends AbstractMuleTestCase
{

    private ClientWrapper clientWrapper;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        clientWrapper = new ClientWrapper(null);
    }

    public void testIsAegisBinding_nullEndpoint() throws Exception
    {
        assertFalse(clientWrapper.isAegisBinding(null));
    }

    public void testIsAegisBinding_NotACxfConnector() throws Exception
    {
        ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class);

        assertFalse(clientWrapper.isAegisBinding(endpoint));
    }

    public void testIsAegisBinding_UsingFrontendFromEndpoing() throws Exception
    {
        ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class);
        Connector cxfConnector = mock(CxfConnector.class);

        when(endpoint.getConnector()).thenReturn(cxfConnector);

        when(endpoint.getProperty(CxfConstants.FRONTEND)).thenReturn(CxfConstants.AEGIS_FRONTEND);
        assertTrue(clientWrapper.isAegisBinding(endpoint));

        when(endpoint.getProperty(CxfConstants.FRONTEND)).thenReturn(CxfConstants.JAX_WS_FRONTEND);
        assertFalse(clientWrapper.isAegisBinding(endpoint));
    }

    public void testIsAegisBinding_UsingDefaultFrontend() throws Exception
    {
        ImmutableEndpoint endpoint = mock(ImmutableEndpoint.class);
        CxfConnector cxfConnector = mock(CxfConnector.class);

        when(endpoint.getConnector()).thenReturn(cxfConnector);
        when(endpoint.getProperty(CxfConstants.FRONTEND)).thenReturn(null);

        when(cxfConnector.getDefaultFrontend()).thenReturn(CxfConstants.AEGIS_FRONTEND);
        assertTrue(clientWrapper.isAegisBinding(endpoint));

        when(cxfConnector.getDefaultFrontend()).thenReturn(CxfConstants.JAX_WS_FRONTEND);
        assertFalse(clientWrapper.isAegisBinding(endpoint));
    }
}
