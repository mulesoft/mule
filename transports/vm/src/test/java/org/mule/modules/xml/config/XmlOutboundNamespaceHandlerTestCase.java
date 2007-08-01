/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.config;

import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.routing.outbound.FilteringXmlMessageSplitter;
import org.mule.routing.outbound.RoundRobinXmlSplitter;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.UMOModel;
import org.mule.util.properties.Dom4jPropertyExtractor;

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
        UMOModel model = managementContext.getRegistry().lookupModel("xml outbound namespace tests");
        UMOComponent component = model.getComponent(name);
        UMODescriptor descriptor = component.getDescriptor();
        List routers = descriptor.getOutboundRouter().getRouters();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0).getClass().getName(), clazz.isAssignableFrom(routers.get(0).getClass()));
        return routers.get(0);
    }

    protected void assertOk(FilteringXmlMessageSplitter splitter, int correln)
    {
        assertEquals(correln, splitter.getEnableCorrelation());
        assertEquals("external", splitter.getExternalSchemaLocation());
        assertTrue(splitter.getPropertyExtractor().getClass().getName(), splitter.getPropertyExtractor() instanceof Dom4jPropertyExtractor);
        assertEquals("/expression", splitter.getSplitExpression());
        assertTrue(splitter.isHonorSynchronicity());
        assertTrue(splitter.isValidateSchema());
        Map namespaces = splitter.getNamespaces();
        assertEquals(1, namespaces.size());
        assertEquals("foo", namespaces.get("bar"));
    }

}