/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.routing.MatchableMessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class XmlFilterNamespaceHandlerServiceTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xml-filter-functional-test-service.xml";
    }

    @Test
    public void testIsXmlFilter() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("test for xml");

        List<MatchableMessageProcessor> routers = ((OutboundRouterCollection) (service).getOutboundMessageProcessor()).getRoutes();

        assertEquals(2, routers.size());
        assertTrue(routers.get(0).getClass().getName(), routers.get(0) instanceof FilteringOutboundRouter);
        assertTrue(((FilteringOutboundRouter) routers.get(0)).getFilter() instanceof IsXmlFilter);
        assertTrue(routers.get(1).getClass().getName(), routers.get(1) instanceof FilteringOutboundRouter);
        assertTrue(((FilteringOutboundRouter) routers.get(1)).getFilter() instanceof NotFilter);
        assertTrue(((NotFilter) ((FilteringOutboundRouter) routers.get(1)).getFilter()).getFilter() instanceof IsXmlFilter);

    }

    @Test
    public void testJXPathFilter()
    {
        Service service = muleContext.getRegistry().lookupService("filter xml for content");

        List<MatchableMessageProcessor> routers = ((OutboundRouterCollection) (service).getOutboundMessageProcessor()).getRoutes();

        assertEquals(1, routers.size());
        assertTrue(routers.get(0).getClass().getName(), routers.get(0) instanceof FilteringOutboundRouter);
        assertTrue(((FilteringOutboundRouter) routers.get(0)).getFilter() instanceof JXPathFilter);
        JXPathFilter filter = (JXPathFilter) ((FilteringOutboundRouter) routers.get(0)).getFilter();
        assertEquals("filter xml for content", filter.getExpectedValue());
        assertEquals("/mule:mule/mule:model/mule:service[2]/@name", filter.getPattern());
        assertNotNull(filter.getNamespaces());
        Map<?, ?> namespaces = filter.getNamespaces();
        assertEquals(2, namespaces.size());
        assertEquals("http://www.springframework.org/schema/beans", namespaces.get("spring"));
        assertTrue(namespaces.get("mule").toString().startsWith("http://www.mulesoft.org/schema/mule/core"));

    }
}
