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
import org.mule.exception.AbstractExceptionListener;
import org.mule.exception.DefaultServiceExceptionStrategy;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class ExceptionListenerTestCase extends AbstractMuleTestCase
{
    public void testAddGoodEndpoint() throws Exception
    {
        AbstractExceptionListener router = new DefaultServiceExceptionStrategy(muleContext);
        OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
        router.addEndpoint(endpoint);
        assertNotNull(router.getMessageProcessors());
        assertTrue(router.getMessageProcessors().contains(endpoint));
    }

    public void testSetGoodEndpoints() throws Exception
    {
        List<OutboundEndpoint> list = new ArrayList<OutboundEndpoint>();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        
        AbstractExceptionListener router = new DefaultServiceExceptionStrategy(muleContext);
        assertNotNull(router.getMessageProcessors());
        assertEquals(0, router.getMessageProcessors().size());
        
        router.addEndpoint(getTestOutboundEndpoint("test"));
        assertEquals(1, router.getMessageProcessors().size());
        
        router.setMessageProcessors(list);
        assertNotNull(router.getMessageProcessors());
        assertEquals(2, router.getMessageProcessors().size());
    }
}
