/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.xml.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.api.processor.MessageProcessorChain;
import org.mule.runtime.core.construct.Flow;
import org.mule.runtime.core.routing.MessageFilter;
import org.mule.runtime.core.routing.filters.logic.NotFilter;
import org.mule.runtime.module.xml.filters.IsXmlFilter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class XmlFilterNamespaceHandlerTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/xml-filter-functional-test-flow.xml";
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
        Object serviceFlow = muleContext.getRegistry().lookupObject("test for xml");

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
}
