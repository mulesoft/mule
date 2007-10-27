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

import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.routing.outbound.RoundRobinXmlSplitter;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOComponent;

import java.util.List;
import java.util.Map;

public class XmlOutboundNamespaceHandlerTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "xml/xml-outbound-namespace-test.xml";
    }

    public void testMessageSplitter()
    {
        FilteringXmlMessageSplitter splitter =
                (FilteringXmlMessageSplitter) getRouter("fancy config splitter", FilteringXmlMessageSplitter.class);
        assertOk(splitter, AbstractOutboundRouter.ENABLE_CORRELATION_ALWAYS);
    }

    public void testRoundRobin()
    {
        RoundRobinXmlSplitter splitter =
                (RoundRobinXmlSplitter) getRouter("fancy config round robin", RoundRobinXmlSplitter.class);
        assertOk(splitter, AbstractOutboundRouter.ENABLE_CORRELATION_IF_NOT_SET);
        assertTrue(splitter.isEnableEndpointFiltering());
    }

    protected Object getRouter(String name, Class clazz)
    {
        UMOComponent component = managementContext.getRegistry().lookupComponent(name);
        List routers = component.getOutboundRouter().getRouters();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0).getClass().getName(), clazz.isAssignableFrom(routers.get(0).getClass()));
        return routers.get(0);
    }

    protected void assertOk(FilteringXmlMessageSplitter splitter, int correln)
    {
        assertEquals(correln, splitter.getEnableCorrelation());
        assertEquals("external", splitter.getExternalSchemaLocation());
        assertEquals("/expression", splitter.getSplitExpression());
        assertTrue(splitter.isHonorSynchronicity());
        assertTrue(splitter.isValidateSchema());
        Map namespaces = splitter.getNamespaces();
        assertEquals(1, namespaces.size());
        assertEquals("foo", namespaces.get("bar"));
    }

}
