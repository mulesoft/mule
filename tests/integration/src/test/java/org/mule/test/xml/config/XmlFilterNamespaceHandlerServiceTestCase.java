/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.config;

import static org.junit.Assert.assertEquals;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.filters.logic.NotFilter;
import org.mule.runtime.module.xml.filters.IsXmlFilter;

import java.util.List;

import org.junit.Test;

public class XmlFilterNamespaceHandlerServiceTestCase extends AbstractIntegrationTestCase
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
}
