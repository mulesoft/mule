/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule;

import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.exception.AbstractExceptionStrategy;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

public class ExceptionListenerTestCase extends AbstractMuleTestCase
{
    public void testAddGoodEndpoint() throws Exception
    {
        AbstractExceptionStrategy router = new DefaultMessagingExceptionStrategy(muleContext, true);
        OutboundEndpoint endpoint = getTestOutboundEndpoint("test");
        router.addEndpoint(endpoint);
        assertNotNull(router.getMessageProcessors());
        assertTrue(router.getMessageProcessors().contains(endpoint));
    }

    public void testSetGoodEndpoints() throws Exception
    {
        List<MessageProcessor> list = new ArrayList<MessageProcessor>();
        list.add(getTestOutboundEndpoint("test"));
        list.add(getTestOutboundEndpoint("test"));
        
        AbstractExceptionStrategy router = new DefaultMessagingExceptionStrategy(muleContext, true);
        assertNotNull(router.getMessageProcessors());
        assertEquals(0, router.getMessageProcessors().size());
        
        router.addEndpoint(getTestOutboundEndpoint("test"));
        assertEquals(1, router.getMessageProcessors().size());
        
        router.setMessageProcessors(list);
        assertNotNull(router.getMessageProcessors());
        assertEquals(2, router.getMessageProcessors().size());
    }
}
