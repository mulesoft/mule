/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.rss;

import org.mule.api.service.Service;
import org.mule.module.rss.endpoint.RssInboundEndpoint;
import org.mule.module.rss.routing.FeedSplitter;
import org.mule.service.ServiceCompositeMessageSource;
import org.mule.tck.FunctionalTestCase;

import java.text.SimpleDateFormat;

import junit.framework.Assert;

public class NamespaceTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "namespace-config.xml";
    }

    public void testEndpointConfig() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("test");
        assertNotNull(service);
        assertTrue(((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0) instanceof RssInboundEndpoint);
        RssInboundEndpoint ep = (RssInboundEndpoint) ((ServiceCompositeMessageSource) service.getMessageSource()).getEndpoints().get(0);
        assertEquals(FeedSplitter.class, ep.getMessageProcessors().get(0).getClass());

        assertNotNull(ep.getLastUpdate());
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        Assert.assertEquals(sdf.parse("2009-10-01"), ep.getLastUpdate());
    }
}
