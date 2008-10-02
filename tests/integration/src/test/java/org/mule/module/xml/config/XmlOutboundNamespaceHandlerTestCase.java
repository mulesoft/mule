/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.config;

import org.mule.api.service.Service;
import org.mule.routing.outbound.AbstractOutboundRouter;
import org.mule.tck.FunctionalTestCase;
import org.mule.module.xml.routing.XmlMessageSplitter;

import java.util.List;
import java.util.Map;

public class XmlOutboundNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/module/xml/xml-outbound-namespace-test.xml";
    }

    public void testMessageSplitter()
    {
        XmlMessageSplitter splitter =
                (XmlMessageSplitter) getRouter("fancy config splitter", XmlMessageSplitter.class);

        assertEquals(AbstractOutboundRouter.ENABLE_CORRELATION_ALWAYS, splitter.getEnableCorrelation());
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
        List routers = service.getOutboundRouter().getRouters();
        assertEquals(1, routers.size());
        assertTrue(routers.get(0).getClass().getName(), clazz.isAssignableFrom(routers.get(0).getClass()));
        return routers.get(0);
    }

}
