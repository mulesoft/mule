/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.config;

import org.mule.routing.filters.logic.NotFilter;
import org.mule.routing.filters.xml.IsXmlFilter;
import org.mule.routing.filters.xml.JXPathFilter;
import org.mule.routing.outbound.FilteringOutboundRouter;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOComponent;

import java.util.List;
import java.util.Map;

public class XmlFilterNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "xml/xml-filter-functional-test.xml";
    }

    /**
     * IsXmlFilter doesn't have any properties to test, so just check it is created
     */
    public void testIsXmlFilter()
    {
        UMOComponent component = managementContext.getRegistry().lookupComponent("test for xml");
        List routers = component.getOutboundRouter().getRouters();
        assertEquals(2, routers.size());
        assertTrue(routers.get(0).getClass().getName(), routers.get(0) instanceof FilteringOutboundRouter);
        assertTrue(((FilteringOutboundRouter) routers.get(0)).getFilter() instanceof IsXmlFilter);
        assertTrue(routers.get(1).getClass().getName(), routers.get(1) instanceof FilteringOutboundRouter);
        assertTrue(((FilteringOutboundRouter) routers.get(1)).getFilter() instanceof NotFilter);
        assertTrue(((NotFilter) ((FilteringOutboundRouter) routers.get(1)).getFilter()).getFilter() instanceof IsXmlFilter);
    }

    public void testJXPathFilter()
    {
        UMOComponent component = managementContext.getRegistry().lookupComponent("filter xml for content");
        List routers = component.getOutboundRouter().getRouters();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0).getClass().getName(), routers.get(0) instanceof FilteringOutboundRouter);
        assertTrue(((FilteringOutboundRouter) routers.get(0)).getFilter() instanceof JXPathFilter);
        JXPathFilter filter = (JXPathFilter) ((FilteringOutboundRouter) routers.get(0)).getFilter();
        assertEquals("filter xml for content", filter.getExpectedValue());
        assertEquals("/mule:mule/mule:model/mule:service[2]/@name", filter.getPattern());
        assertNotNull(filter.getNamespaces());
        Map namespaces = filter.getNamespaces();
        assertEquals(2, namespaces.size());
        assertEquals("http://www.springframework.org/schema/beans", namespaces.get("spring"));
        assertEquals("http://www.mulesource.org/schema/mule/core/2.0", namespaces.get("mule"));
    }

}
