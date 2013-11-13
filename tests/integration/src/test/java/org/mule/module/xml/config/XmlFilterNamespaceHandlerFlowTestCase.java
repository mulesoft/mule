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

import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.construct.Flow;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.logic.NotFilter;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class XmlFilterNamespaceHandlerFlowTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xml-filter-functional-test-flow.xml";
    }

    @Test
    public void testIsXmlFilter() throws Exception
    {
        Object flow = muleContext.getRegistry().lookupObject("test for xml");
        MessageProcessorChain notXmlSubFlow;
        List<MessageProcessor> outEndpoints = new ArrayList<MessageProcessor>(2);

        outEndpoints.add(((Flow) flow).getMessageProcessors().get(0));
        notXmlSubFlow = muleContext.getRegistry().lookupObject("notXml");
        outEndpoints.add((notXmlSubFlow.getMessageProcessors().get(0)));

        assertEquals(2, outEndpoints.size());
        assertTrue(outEndpoints.get(0).getClass().getName(), outEndpoints.get(0) instanceof MessageFilter);
        assertTrue(((MessageFilter) outEndpoints.get(0)).getFilter() instanceof IsXmlFilter);
        assertTrue(outEndpoints.get(1).getClass().getName(), outEndpoints.get(1) instanceof MessageFilter);
        assertTrue(((MessageFilter) outEndpoints.get(1)).getFilter() instanceof NotFilter);
        assertTrue(((NotFilter) ((MessageFilter) outEndpoints.get(1)).getFilter()).getFilter() instanceof IsXmlFilter);

    }

    @Test
    public void testJXPathFilter()
    {
        Object flow = muleContext.getRegistry().lookupObject("filter xml for content");

        List<MessageProcessor> outEndpoints = new ArrayList<MessageProcessor>(1);
        outEndpoints.add(((Flow) flow).getMessageProcessors().get(0));

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
        assertTrue(namespaces.get("mule").toString().startsWith("http://www.mulesoft.org/schema/mule/core"));

    }
}
