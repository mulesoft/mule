/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.jms;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.service.Service;
import org.mule.api.source.MessageSource;
import org.mule.construct.Flow;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.jms.filters.JmsSelectorFilter;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertNotNull;

public class JmsSelectorDetectionTestCase extends AbstractServiceAndFlowTestCase
{

    public JmsSelectorDetectionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setStartContext(false);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][] {
                {ConfigVariant.SERVICE, "jms-selector-detection-service.xml"},
                {ConfigVariant.FLOW, "jms-selector-detection-flow.xml"}
        });
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
        if (source instanceof InboundEndpoint)
        {
            ep = (InboundEndpoint) source;
        }
        else if (source instanceof ServiceCompositeMessageSource)
        {
            ep = ((ServiceCompositeMessageSource) source).getEndpoints().get(0);
        }

        return ep;
    }

    private MessageSource getSource()
    {
        MessageSource source;
        Object flowOrService = muleContext.getRegistry().lookupObject("TestSelector");
        assertNotNull(flowOrService);
        if (flowOrService instanceof Service)
        {
            Service svc = (Service) flowOrService;
            source = svc.getMessageSource();
        }
        else
        {
            Flow flow = (Flow) flowOrService;
            source = flow.getMessageSource();
        }

        return source;
    }

}
