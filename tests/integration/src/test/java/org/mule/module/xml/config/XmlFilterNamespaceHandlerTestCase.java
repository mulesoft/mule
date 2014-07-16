/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.config;

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.MatchableMessageProcessor;
import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.construct.Flow;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class XmlFilterNamespaceHandlerTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/module/xml/xml-filter-functional-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/module/xml/xml-filter-functional-test-flow.xml"}});
    }

    public XmlFilterNamespaceHandlerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * IsXmlFilter doesn't have any properties to test, so just check it is created
     *
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws NoSuchFieldException
     * @throws SecurityException
     */
    @Test
    public void testIsXmlFilter()
        throws IllegalArgumentException, IllegalAccessException, SecurityException, NoSuchFieldException
    {
        Object serviceFlow =  muleContext.getRegistry().lookupObject("test for xml");

        if (serviceFlow instanceof Service)
        {
            List<MatchableMessageProcessor> routers =
                ((OutboundRouterCollection) ((Service) serviceFlow).getOutboundMessageProcessor()).getRoutes();

            assertEquals(2, routers.size());
            assertTrue(routers.get(0).getClass().getName(), routers.get(0) instanceof FilteringOutboundRouter);
            assertTrue(((FilteringOutboundRouter) routers.get(0)).getFilter() instanceof IsXmlFilter);
            assertTrue(routers.get(1).getClass().getName(), routers.get(1) instanceof FilteringOutboundRouter);
            assertTrue(((FilteringOutboundRouter) routers.get(1)).getFilter() instanceof NotFilter);
            assertTrue(((NotFilter) ((FilteringOutboundRouter) routers.get(1)).getFilter()).getFilter() instanceof IsXmlFilter);

        }
        else if (serviceFlow instanceof Flow)
        {
            Field f;
            MessageProcessorChain notXmlSubFlow;
            List<MessageProcessor> outEndpoints = new ArrayList<MessageProcessor>(2);

            outEndpoints.add(((Flow) serviceFlow).getMessageProcessors().get(0));
            notXmlSubFlow = muleContext.getRegistry().lookupObject("notXml");
            outEndpoints.add((notXmlSubFlow.getMessageProcessors().get(0)));

            assertEquals(2, outEndpoints.size());
            assertTrue(outEndpoints.get(0).getClass().getName(), outEndpoints.get(0) instanceof MessageFilter);
            assertTrue(((MessageFilter) outEndpoints.get(0)).getFilter() instanceof IsXmlFilter);
            assertTrue(outEndpoints.get(1).getClass().getName(), outEndpoints.get(1) instanceof MessageFilter);
            assertTrue(((MessageFilter) outEndpoints.get(1)).getFilter() instanceof NotFilter);
            assertTrue(((NotFilter) ((MessageFilter) outEndpoints.get(1)).getFilter()).getFilter() instanceof IsXmlFilter);
        }
        else
        {
            fail("Unexpected Object");
        }
    }

    @Test
    public void testJXPathFilter()
    {
        Object serviceFlow = null;

        serviceFlow = muleContext.getRegistry().lookupObject("filter xml for content");

        if (serviceFlow instanceof Service)
        {

            List<MatchableMessageProcessor> routers =
                ((OutboundRouterCollection) ((Service) serviceFlow).getOutboundMessageProcessor()).getRoutes();
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
            assertTrue(namespaces.get("mule")
                .toString()
                .startsWith("http://www.mulesoft.org/schema/mule/core"));

        }
        else if (serviceFlow instanceof Flow)
        {

            List<MessageProcessor> outEndpoints = new ArrayList<MessageProcessor>(1);
            outEndpoints.add(((Flow) serviceFlow).getMessageProcessors().get(0));

            assertEquals(1, outEndpoints.size());
            assertTrue(outEndpoints.get(0).getClass().getName(), outEndpoints.get(0) instanceof MessageFilter);

            assertTrue(((MessageFilter) outEndpoints.get(0)).getFilter() instanceof JXPathFilter);
            JXPathFilter filter = (JXPathFilter) ((MessageFilter) outEndpoints.get(0)).getFilter();
            assertEquals("filter xml for content", filter.getExpectedValue());
            assertEquals("/mule:mule/mule:flow[2]/@name", filter.getPattern());
            assertNotNull(filter.getNamespaces());
            Map<?, ?> namespaces = filter.getNamespaces();
            assertEquals(2, namespaces.size());
            assertEquals("http://www.springframework.org/schema/beans", namespaces.get("spring"));
            assertTrue(namespaces.get("mule")
                .toString()
                .startsWith("http://www.mulesoft.org/schema/mule/core"));
        }
        else
        {
            fail("Unexpected Object");
        }
    }
}
