/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.jms;

import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.endpoint.InboundEndpoint;
import org.mule.runtime.core.api.source.MessageSource;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.transport.jms.filters.JmsSelectorFilter;

import org.junit.Test;

public class JmsSelectorDetectionTestCase extends FunctionalTestCase
{

    public JmsSelectorDetectionTestCase()
    {
        setStartContext(false);
    }

    @Override
    protected String getConfigFile()
    {
        return "jms-selector-detection-flow.xml";
    }

    @Test
    public void testDetectsSelector() throws Exception
    {
        MessageSource source = getSource();
        InboundEndpoint ep = null;
        ep = getEnpoint(source, ep);

        JmsConnector connector = (JmsConnector) muleContext.getRegistry().lookupConnector("jmsConnector");
        JmsSelectorFilter selector = connector.getSelector(ep);
        assertNotNull(selector);
    }

    private InboundEndpoint getEnpoint(MessageSource source, InboundEndpoint ep)
    {
        return (InboundEndpoint) source;
    }

    private MessageSource getSource()
    {
        Object flowOrService = muleContext.getRegistry().lookupObject("TestSelector");
        assertNotNull(flowOrService);
        Flow flow = (Flow) flowOrService;
        return flow.getMessageSource();
    }

}
