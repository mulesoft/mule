/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.endpoint;

import org.mule.api.MuleException;
import org.mule.api.endpoint.ImmutableEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.testmodels.mule.TestMessageProcessor;

import java.util.List;

public class EndpointMessageProcessorsTestCase extends AbstractEndpointTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/endpoint/endpoint-message-processors.xml";
    }

    public void testGlobalEndpoint1() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("ep1");
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(1, processors.size());
        assertTrue(processors.get(0) instanceof TestMessageProcessor);
    }

    public void testGlobalEndpoint2() throws MuleException
    {
        ImmutableEndpoint endpoint = doTest("ep2");
        List <MessageProcessor> processors = endpoint.getMessageProcessors();
        assertNotNull(processors);
        assertEquals(3, processors.size());
        assertEquals("1", ((TestMessageProcessor) processors.get(0)).getLabel());
        assertEquals("2", ((TestMessageProcessor) processors.get(1)).getLabel());
        assertEquals("3", ((TestMessageProcessor) processors.get(2)).getLabel());
    }

}
