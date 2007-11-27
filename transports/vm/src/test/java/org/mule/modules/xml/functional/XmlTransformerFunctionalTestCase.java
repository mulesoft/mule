/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.xml.functional;

import org.mule.extras.client.MuleClient;
import org.mule.umo.UMOException;
import org.mule.umo.UMOMessage;

import org.custommonkey.xmlunit.XMLAssert;
import org.w3c.dom.Document;

public class XmlTransformerFunctionalTestCase extends AbstractXmlFunctionalTestCase
{

    public static final String SIMPLE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<parent><child name=\"poot\"/></parent>";
    public static final String CHILDLESS_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<parent/>";
    public static final String SERIALIZED = "<org.mule.modules.xml.functional.XmlTransformerFunctionalTestCase_-Parent>\n" +
            "  <child/>\n" +
            "</org.mule.modules.xml.functional.XmlTransformerFunctionalTestCase_-Parent>";

    protected String getConfigResources()
    {
        return "xml/xml-transformer-functional-test.xml";
    }

    protected MuleClient sendXml() throws UMOException
    {
        MuleClient client = new MuleClient();
        client.dispatch("xml-in", SIMPLE_XML, null);
        return client;
    }

    protected MuleClient sendObject() throws UMOException
    {
        return sendObject("object-in");
    }

    protected MuleClient sendObject(String endpoint) throws UMOException
    {
        MuleClient client = new MuleClient();
        client.dispatch(endpoint, new Parent(new Child()), null);
        return client;
    }

    public void testXmlOut() throws Exception
    {
        String xml = (String) receive(sendXml(), "xml-out", String.class);
        XMLAssert.assertXMLEqual(SIMPLE_XML, xml);
    }

    public void testXmlDomOut() throws UMOException
    {
        Document dom = (Document) receive(sendXml(), "xml-dom-out", Document.class);
        assertEquals("parent", dom.getDocumentElement().getLocalName());
    }

    public void testXmlXsltOut() throws Exception
    {
        String xml = (String) receive(sendXml(), "xml-xslt-out-string", String.class);
        XMLAssert.assertXMLEqual(CHILDLESS_XML, xml);
    }

    public void testDomXmlOut() throws Exception
    {
        String xml = (String) receive(sendXml(), "dom-xml-out", String.class);
        XMLAssert.assertXMLEqual(SIMPLE_XML, xml);
    }

    public void testObjectOut() throws Exception
    {
        receive(sendObject(), "object-out", Parent.class);
    }

    public void testObjectXmlOut() throws Exception
    {
        String xml = (String) receive(sendObject(), "object-xml-out", String.class);
        System.out.println(xml);
        XMLAssert.assertXMLEqual(SERIALIZED, xml);
    }

    public void testXmlObjectOut() throws UMOException
    {
        receive(sendObject(), "xml-object-out", Parent.class);
    }

    public void testXmlJxpathOut() throws Exception
    {
        String xml = (String) receive(sendXml(), "xml-jxpath-out", String.class);
        assertEquals("1", xml);
    }


    protected Object receive(MuleClient client, String endpoint, Class clazz) throws UMOException
    {
        UMOMessage message = client.receive(endpoint, TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayload());
        assertTrue(message.getPayload().getClass().getName(), clazz.isAssignableFrom(message.getPayload().getClass()));
        return message.getPayload();
    }


    public static class Parent
    {
        private Child child;

        public Parent()
        {
            this(null);
        }

        public Parent(Child child)
        {
            setChild(child);
        }

        public Child getChild()
        {
            return child;
        }

        public void setChild(Child child)
        {
            this.child = child;
        }
    }

    public static class Child
    {

    }

}
