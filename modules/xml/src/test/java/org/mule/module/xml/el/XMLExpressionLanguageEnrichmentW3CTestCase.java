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

import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.w3c.dom.Document;

public class XMLExpressionLanguageEnrichmentW3CTestCase extends AbstractELTestCase
{

    public XMLExpressionLanguageEnrichmentW3CTestCase(Variant variant)
    {
        super(variant);
    }

    @Test
    public void addElementText() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setTextContent('myText')", message);
        evaluate("xpath('/root').setTextContent('myText')", message);
        assertTrue(message.getPayloadAsString().contains("<root>myText</root>"));
    }

    @Test
    public void addElementTextInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setTextContent(1)", message);
        evaluate("xpath('/root').setTextContent(1)", message);
        assertTrue(message.getPayloadAsString().contains("<root>1</root>"));
    }

    @Test
    public void addElementTextNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"1\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setTextContent(xpath('/root/@attr'))", message);
        evaluate("xpath('/root').setTextContent(xpath('/root/@attr'))", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\">1</root>"));
    }

    @Test
    public void replaceElementText() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root>oldText</root>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setTextContent('myText')", message);
        evaluate("xpath('/root').setTextContent('myText')", message);
        assertTrue(message.getPayloadAsString().contains("<root>myText</root>"));
    }

    @Test
    public void setAttribute() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setAttribute('newAttr','attrValue')", message);
        evaluate("xpath('/root').setAttribute('newAttr','attrValue')", message);
        assertTrue(message.getPayloadAsString().contains("<root newAttr=\"attrValue\"/>"));
    }

    @Test
    public void setAttributeInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setAttribute('newAttr',1)", message);
        evaluate("xpath('/root').setAttribute('newAttr',1)", message);
        assertTrue(message.getPayloadAsString().contains("<root newAttr=\"1\"/>"));
    }

    @Test
    public void setAttributeNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').setAttribute('newAttr',xpath('/root/@other'))", message);
        evaluate("xpath('/root').setAttribute('newAttr',xpath('/root/@other'))", message);
        assertTrue(XMLUnit.compareXML("<root other=\"1\" newAttr=\"1\"/>", message.getPayloadAsString())
            .identical());
    }

    @Test
    public void updateAttributeValue() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').setTextContent('newValue')", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"newValue\"/>"));
    }

    @Test
    public void updateAttributeValueInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').setTextContent('1')", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\"/>"));
    }

    @Test
    public void updateAttributeValueNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\" attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').setTextContent(xpath('/root/@other'))", message);
        assertTrue(XMLUnit.compareXML("<root other=\"1\" attr=\"1\"/>", message.getPayloadAsString())
            .identical());
    }

}
