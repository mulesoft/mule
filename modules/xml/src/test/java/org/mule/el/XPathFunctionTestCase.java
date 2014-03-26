/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.el;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertSame;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.construct.Flow;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.IOUtils;

import java.util.List;

import org.dom4j.Document;
import org.dom4j.Element;
import org.junit.Test;

public class XPathFunctionTestCase extends FunctionalTestCase
{

    private String payload;

    @Override
    protected String getConfigFile()
    {
        return "xpath-function-test-config.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
        payload = IOUtils.getResourceAsString("cd-catalog.xml", getClass());
    }

    @Test
    public void getTitles() throws Exception
    {
        MuleEvent event = runFlow("getTitles", payload);
        assertTitleElements((List<Element>) event.getMessage().getPayload());
    }

    @Test
    public void getTitlesFromStream() throws Exception
    {
        MuleEvent event = runFlow("getTitlesFromStream", payload);
        assertTrue(event.getMessage().getPayload() instanceof Document);
    }

    @Test
    public void getTitlesFromFlowVar() throws Exception
    {
        MuleEvent event = getTestEvent(payload);
        event.setFlowVariable("xml", payload);
        event = ((Flow) getFlowConstruct("getTitlesFromFlowVar")).process(event);

        List<Element> elements = event.getFlowVariable("titles");

        assertTitlesCount(elements);
        assertSame(payload, event.getMessage().getPayload());
    }

    @Test
    public void getTitlesFromCustomPayload() throws Exception
    {
        MuleEvent event = runFlow("getTitlesFromCustomPayload", payload);
        List<Document> documents = event.getFlowVariable("titles");
        assertTitlesCount(documents);
        assertSame(payload, event.getMessage().getPayload());
    }

    @Test(expected = MessagingException.class)
    public void emptyXPathExpression() throws Exception
    {
        runFlow("emptyXPathExpression", payload);
    }

    @Test(expected = MessagingException.class)
    public void noArgsAtAll() throws Exception
    {
        runFlow("noArgsAtAll", payload);
    }

    @Test
    public void textNotSeparated() throws Exception
    {
        payload = IOUtils.getResourceAsString("sswa2smtp.xml", getClass());
        runFlow("textNotSeparated", payload);
    }

    private void assertTitleElements(List<Element> elements)
    {
        assertTitlesCount(elements);
        for (Element element : elements)
        {
            assertEquals("title", element.getName());
        }
    }

    private void assertTitlesCount(List<?> elements)
    {
        assertNotNull(elements);
        assertEquals(26, elements.size());
    }
}
