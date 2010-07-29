/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.parsers.specific;

import org.mule.api.MuleException;
import org.mule.api.endpoint.InboundEndpoint;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.FunctionalTestCase;

public class FlowDefinitionParserTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/config/spring/parsers/specific/flow.xml";
    }

    public void testFlow() throws MuleException, Exception
    {
        SimpleFlowConstruct flow = muleContext.getRegistry().lookupObject("flow");
        assertEquals(DefaultInboundEndpoint.class, flow.getMessageSource().getClass());
        assertEquals("test://test", ((InboundEndpoint) flow.getMessageSource()).getEndpointURI()
            .getUri()
            .toString());
        assertEquals(3, flow.getMessageProcessors().size());
    }

}
