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
import org.mule.construct.Flow;
import org.mule.module.xml.filters.IsXmlFilter;
import org.mule.module.xml.filters.JXPathFilter;
import org.mule.routing.MessageFilter;
import org.mule.routing.filters.logic.NotFilter;
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
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("test for xml");

        List<MessageProcessor> processors = flow.getMessageProcessors();

        assertEquals(2, processors.size());
        assertEquals(IsXmlFilter.class, ((MessageFilter) processors.get(0)).getFilter().getClass());
        assertEquals(NotFilter.class, ((MessageFilter) processors.get(1)).getFilter().getClass());
        assertEquals(IsXmlFilter.class, ((MessageFilter) processors.get(0)).getFilter().getClass());

    }

    @Test
    public void testJXPathFilter()
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("filter xml for content");

        List<MessageProcessor> processors = service.getMessageProcessors();

        assertEquals(1, processors.size());
        assertEquals(JXPathFilter.class, ((MessageFilter) processors.get(0)).getFilter().getClass());
        JXPathFilter filter = (JXPathFilter) ((MessageFilter) processors.get(0)).getFilter();
        assertEquals("filter xml for content", filter.getExpectedValue());
        assertEquals("/mule:mule/mule:model/mule:service[2]/@name", filter.getPattern());
        assertNotNull(filter.getNamespaces());
        Map<?, ?> namespaces = filter.getNamespaces();
        assertEquals(2, namespaces.size());
        assertEquals("http://www.springframework.org/schema/beans", namespaces.get("spring"));
        assertTrue(namespaces.get("mule").toString().startsWith("http://www.mulesoft.org/schema/mule/core"));

    }
}
