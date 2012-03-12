/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.el;

import static org.junit.Assert.assertTrue;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.el.context.AbstractELTestCase;
import org.mule.tck.size.SmallTest;

import org.dom4j.Document;
import org.junit.Test;

@SmallTest
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
        evaluate("xpath('/root').text='myText'", message);
        assertTrue(message.getPayloadAsString().contains("<root>myText</root>"));
    }

    @Test
    public void addElementTextInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').text=1", message);
        assertTrue(message.getPayloadAsString().contains("<root>1</root>"));
    }

    @Test
    public void addElementTextNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"1\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').text=xpath('/root/@attr')", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\">1</root>"));
    }

    @Test
    public void replaceElementText() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root>oldText</root>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').text='myText'", message);
        assertTrue(message.getPayloadAsString().contains("<root>myText</root>"));
    }

    @Test
    public void replaceElementTextInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root>oldText</root>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').text=1", message);
        assertTrue(message.getPayloadAsString().contains("<root>1</root>"));
    }

    @Test
    public void replaceElementTextNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"1\">oldText</root>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').text=xpath('/root/@attr')", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\">1</root>"));
    }

    @Test
    public void updateAttributeValue() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').text='newValue'", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"newValue\"/>"));
    }

    @Test
    public void updateAttributeValueInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').text=1", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\"/>"));
    }

    @Test
    public void updateAttributeValueNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\" attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root/@attr').text=xpath('/root/@other')", message);
        assertTrue(message.getPayloadAsString().contains("<root other=\"1\" attr=\"1\"/>"));
    }

    @Test
    public void updateAttributeValueAlternative() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').attributes['attr']='newValue'", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"newValue\"/>"));
    }

    @Test
    public void updateAttributeValueAlternativeInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').attributes['attr']=1", message);
        assertTrue(message.getPayloadAsString().contains("<root attr=\"1\"/>"));
    }

    @Test
    public void updateAttributeValueAlternativeNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\" attr=\"oldValue\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').attributes['attr']=xpath('/root/@other')", message);
        assertTrue(message.getPayloadAsString().contains("<root other=\"1\" attr=\"1\"/>"));
    }

    @Test
    public void addAttribute() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').attributes['newAttr']='attrValue'", message);
        assertTrue(message.getPayloadAsString().contains("<root newAttr=\"attrValue\"/>"));
    }

    @Test
    public void addAttributeInt() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').attributes['newAttr']=1", message);
        assertTrue(message.getPayloadAsString().contains("<root newAttr=\"1\"/>"));
    }

    @Test
    public void addAttributeNode() throws Exception
    {
        MuleMessage message = new DefaultMuleMessage("<root other=\"1\"/>", muleContext);
        message.setPayload(message.getPayload(Document.class));
        evaluate("xpath('/root').attributes['newAttr']=xpath('/root/@other')", message);
        assertTrue(message.getPayloadAsString().contains("<root other=\"1\" newAttr=\"1\"/>"));
    }

}
