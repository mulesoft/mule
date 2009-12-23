/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.service.DefaultServiceExceptionStrategy;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class ExceptionListenerTestCase extends AbstractMuleTestCase
{
    public void testAddGoodEndpoint() throws Exception
    {
        AbstractExceptionListener router = new DefaultServiceExceptionStrategy();
        OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
        router.addEndpoint(endpoint);
        assertNotNull(router.getEndpoints());
        assertTrue(router.getEndpoints().contains(endpoint));
    }

    public void testSetGoodEndpoints() throws Exception
    {
        List<OutboundEndpoint> list = new ArrayList<OutboundEndpoint>();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        
        AbstractExceptionListener router = new DefaultServiceExceptionStrategy();
        assertNotNull(router.getEndpoints());
        assertEquals(0, router.getEndpoints().size());
        
        router.addEndpoint(getTestOutboundEndpoint("test"));
        assertEquals(1, router.getEndpoints().size());
        
        router.setEndpoints(list);
        assertNotNull(router.getEndpoints());
        assertEquals(2, router.getEndpoints().size());
    }
}
