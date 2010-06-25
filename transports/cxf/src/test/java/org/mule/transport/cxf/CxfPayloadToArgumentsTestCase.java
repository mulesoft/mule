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

import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.tck.AbstractMuleTestCase;

public class CxfPayloadToArgumentsTestCase extends AbstractMuleTestCase
{

    private OutboundEndpoint endpoint;

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        endpoint = mock(OutboundEndpoint.class);
    }

    public void testGetPayloadToArgumentsForEndpoint_null() throws MalformedEndpointException
    {
        when(endpoint.getProperty(CxfConstants.PAYLOAD_TO_ARGUMENTS)).thenReturn(null);
        assertSame(CxfPayloadToArguments.NULL_PAYLOAD_AS_PARAMETER,
            CxfPayloadToArguments.getPayloadToArgumentsForEndpoint(endpoint));
    }

    public void testGetPayloadToArgumentsForEndpoint_NullPayloadAsParameter()
        throws MalformedEndpointException
    {
        when(endpoint.getProperty(CxfConstants.PAYLOAD_TO_ARGUMENTS)).thenReturn(
            CxfConstants.PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_PARAMETER);
        assertSame(CxfPayloadToArguments.NULL_PAYLOAD_AS_PARAMETER,
            CxfPayloadToArguments.getPayloadToArgumentsForEndpoint(endpoint));
    }

    public void testGetPayloadToArgumentsForEndpoint_NullPayloadAsVoid() throws MalformedEndpointException
    {
        when(endpoint.getProperty(CxfConstants.PAYLOAD_TO_ARGUMENTS)).thenReturn(
            CxfConstants.PAYLOAD_TO_ARGUMENTS_NULL_PAYLOAD_AS_VOID);
        assertSame(CxfPayloadToArguments.NULL_PAYLOAD_AS_VOID,
            CxfPayloadToArguments.getPayloadToArgumentsForEndpoint(endpoint));
    }

    public void testGetPayloadToArgumentsForEndpoint_Invalid() throws MalformedEndpointException
    {
        String somethingInvalid = "something invalid";
        when(endpoint.getProperty(CxfConstants.PAYLOAD_TO_ARGUMENTS)).thenReturn(somethingInvalid);

        try
        {
            CxfPayloadToArguments.getPayloadToArgumentsForEndpoint(endpoint);
            fail("It should have thrown an exception");
        }
        catch (MalformedEndpointException e)
        {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains(somethingInvalid));
        }
    }

}
