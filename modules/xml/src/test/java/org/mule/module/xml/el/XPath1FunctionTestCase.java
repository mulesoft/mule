/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.el;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.el.context.AbstractELTestCase;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.StringBufferInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.tree.DefaultAttribute;
import org.junit.Test;

public class XPath1FunctionTestCase extends AbstractELTestCase
{

    public XPath1FunctionTestCase(Variant variant, String optimizer)
    {
        super(variant, optimizer);
    }

    @Test
    public void xpathFunctionString() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root foo=\"bar\"/>", muleContext);
        // We use dom4j internally
        assertEquals(DefaultAttribute.class, evaluate("xpath('/root/@foo')", message).getClass());
    }

    @Test
    public void xpathFunctionStream() throws Exception
    {
        InputStream payload = new ByteArrayInputStream("<root foo=\"bar\"/>".getBytes());
        MuleMessage message = new DefaultMuleMessage(payload, muleContext);
        assertEquals(DefaultAttribute.class, evaluate("xpath('/root/@foo')", message).getClass());
        assertTrue(message.getPayload() instanceof Document);
    }

    @Test
    public void xpathFunctionStringText() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root foo=\"bar\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        assertEquals("bar", evaluate("xpath('/root/@foo').text", message));
    }

    @Test
    public void xpathFunctionDom4j() throws Exception
    {
        Document document = DocumentHelper.parseText("<root foo=\"bar\"/>");
        MuleMessage message = new DefaultMuleMessage(document, muleContext);
        assertEquals(document.getRootElement().attribute("foo"), evaluate("xpath('/root/@foo')", message));
        assertEquals("bar", evaluate("xpath('/root/@foo').text", message));
    }

    @Test
    public void xpathFunctionDom4jText() throws Exception
    {
        Document document = DocumentHelper.parseText("<root foo=\"bar\"/>");
        MuleMessage message = new DefaultMuleMessage(document, muleContext);
        assertEquals("bar", evaluate("xpath('/root/@foo').text", message));
    }

    @Test
    public void xpathFunctionW3C() throws Exception
    {
        org.w3c.dom.Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new StringBufferInputStream("<root foo=\"bar\"/>"));
        MuleMessage message = new DefaultMuleMessage(document, muleContext);
        assertEquals(document.getFirstChild().getAttributes().getNamedItem("foo"),
            evaluate("xpath('/root/@foo')", message));
    }

    @Test
    public void xpathFunctionW3CText() throws Exception
    {
        org.w3c.dom.Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new StringBufferInputStream("<root foo=\"bar\"/>"));
        MuleMessage message = new DefaultMuleMessage(document, muleContext);
        assertEquals("bar", evaluate("xpath('/root/@foo').textContent", message));
    }

}
