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

public class XPath1FallbackFunctionTestCase extends FunctionalTestCase
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
        this.payload = IOUtils.getResourceAsString("cd-catalog.xml", this.getClass());
    }

    @Test
    public void getTitles() throws Exception
    {
        MuleEvent event = this.runFlow("getTitles", this.payload);
        this.assertTitleElements((List<Element>) event.getMessage().getPayload());
    }

    @Test
    public void getTitlesFromStream() throws Exception
    {
        MuleEvent event = this.runFlow("getTitlesFromStream", this.payload);
        assertTrue(event.getMessage().getPayload() instanceof Document);
    }

    @Test
    public void getTitlesFromFlowVar() throws Exception
    {
        MuleEvent event = getTestEvent(this.payload);
        event.setFlowVariable("xml", this.payload);
        event = ((Flow) this.getFlowConstruct("getTitlesFromFlowVar")).process(event);

        List<Element> elements = event.getFlowVariable("titles");

        this.assertTitlesCount(elements);
        assertSame(this.payload, event.getMessage().getPayload());
    }

    @Test
    public void getTitlesFromCustomPayload() throws Exception
    {
        MuleEvent event = this.runFlow("getTitlesFromCustomPayload", this.payload);
        List<Document> documents = event.getFlowVariable("titles");
        assertTitlesCount(documents);
        assertSame(this.payload, event.getMessage().getPayload());
    }

    @Test(expected = MessagingException.class)
    public void emptyXPathExpression() throws Exception
    {
        this.runFlow("emptyXPathExpression", this.payload);
    }

    @Test(expected = MessagingException.class)
    public void noArgsAtAll() throws Exception
    {
        this.runFlow("noArgsAtAll", this.payload);
    }

    private void assertTitleElements(List<Element> elements)
    {
        this.assertTitlesCount(elements);
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
