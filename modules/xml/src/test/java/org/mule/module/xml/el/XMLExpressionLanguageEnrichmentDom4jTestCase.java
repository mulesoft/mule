/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.el;

import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.el.context.AbstractELTestCase;

import org.dom4j.Document;
import org.junit.Test;

public class XMLExpressionLanguageEnrichmentDom4jTestCase extends AbstractELTestCase
{

    public XMLExpressionLanguageEnrichmentDom4jTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void addElementText() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setText('myText')", message);
        evaluate("xpath('/root').setText('myText')", message);
        assertTrue(message.getPayloadAsString().contains("<root>myText</root>"));
    }

    @Test
    public void addElementTextInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setText(1)", message);
        evaluate("xpath('/root').setText(1)", message);
        assertTrue(message.getPayloadAsString().contains("<root>1</root>"));
    }

    @Test
    public void addElementTextNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"1\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setText(xpath('/root/@attr'))", message);
        evaluate("xpath('/root').setText(xpath('/root/@attr'))", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\">1</root>"));
    }

    @Test
    public void replaceElementText() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root>oldText</root>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setText('myText')", message);
        evaluate("xpath('/root').setText('myText')", message);
        assertTrue(message.getPayloadAsString().contains("<root>myText</root>"));
    }

    @Test
    public void addAttribute() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').addAttribute('newAttr','attrValue')", message);
        evaluate("xpath('/root').addAttribute('newAttr','attrValue')", message);
        assertTrue(message.getPayloadAsString().contains("<root newAttr=\"attrValue\"/>"));
    }

    @Test
    public void addAttributeInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').addAttribute('newAttr',1)", message);
        evaluate("xpath('/root').addAttribute('newAttr',1)", message);
        assertTrue(message.getPayloadAsString().contains("<root newAttr=\"1\"/>"));
    }

    @Test
    public void addAttributeNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').addAttribute('newAttr',xpath('/root/@other'))", message);
        evaluate("xpath('/root').addAttribute('newAttr',xpath('/root/@other'))", message);
        assertTrue(message.getPayloadAsString().contains("<root other=\"1\" newAttr=\"1\"/>"));
    }

    @Test
    public void updateAttributeValue() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').setText('newValue')", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"newValue\"/>"));
    }

    @Test
    public void updateAttributeValueInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').setText('1')", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\"/>"));
    }

    @Test
    public void updateAttributeValueNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\" attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').setText(xpath('/root/@other'))", message);
        assertTrue(message.getPayloadAsString().contains("<root other=\"1\" attr=\"1\"/>"));
    }

}
