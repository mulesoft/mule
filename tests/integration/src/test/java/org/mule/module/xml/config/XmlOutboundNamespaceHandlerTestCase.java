/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.config;

import org.mule.api.routing.OutboundRouterCollection;
import org.mule.api.service.Service;
import org.mule.module.xml.routing.XmlMessageSplitter;
import org.mule.routing.CorrelationMode;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.List;
import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class XmlOutboundNamespaceHandlerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-outbound-namespace-test.xml";
    }

    @Test
    public void testMessageSplitter()
    {
        XmlMessageSplitter splitter =
                (XmlMessageSplitter) getRouter("fancy config splitter", XmlMessageSplitter.class);

        assertEquals(CorrelationMode.ALWAYS, splitter.getEnableCorrelation());
        assertEquals("external", splitter.getExternalSchemaLocation());
        assertEquals("/expression", splitter.getSplitExpression());
        assertTrue(splitter.isDeterministic());
        assertTrue(splitter.isValidateSchema());
        Map namespaces = splitter.getNamespaces();
        assertEquals(1, namespaces.size());
        assertEquals("foo", namespaces.get("bar"));
    }

    protected Object getRouter(String name, Class clazz)
    {
        Service service = muleContext.getRegistry().lookupService(name);
        List routers = ((OutboundRouterCollection) service.getOutboundMessageProcessor()).getRoutes();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0).getClass().getName(), clazz.isAssignableFrom(routers.get(0).getClass()));
        return routers.get(0);
    }

}
