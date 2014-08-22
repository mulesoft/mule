/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import static org.junit.Assert.assertEquals;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.File;
import java.io.InputStream;
import java.net.URL;

import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.junit.Test;
import org.xml.sax.InputSource;

public class XmlUtilsTestCase extends AbstractMuleTestCase
{

    private static final String SIMPLE_XML_RESOURCE = "simple.xml";
    private static final String SIMPLE_XML_CONTENT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                                                     + "<just>testing</just>";

    @Test
    public void testConvertsToW3cDocumentFromDom4jDocument() throws Exception
    {
        org.dom4j.Document document = XMLTestUtils.toDom4jDocument(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(document);
    }

    @Test
    public void testConvertsToW3cDocumentFromW3cDocument() throws Exception
    {
        org.w3c.dom.Document document = XMLTestUtils.toW3cDocument(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(document);
    }

    @Test
    public void testConvertsToW3cDocumentFromInputSource() throws Exception
    {
        InputSource payload = XMLTestUtils.toInputSource(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    @Test
    public void testConvertsToW3cDocumentFromSource() throws Exception
    {
        Source payload = XMLTestUtils.toSource(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    @Test
    public void testConvertsToW3cDocumentFromXmlStreamReader() throws Exception
    {
        XMLStreamReader payload = XMLTestUtils.toXmlStreamReader(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    @Test
    public void testConvertsToW3cDocumentFromInputStream() throws Exception
    {
        InputStream payload = XMLTestUtils.toInputStream(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    @Test
    public void testConvertsToW3cDocumentFromString() throws Exception
    {
        String payload = XMLTestUtils.toString(SIMPLE_XML_RESOURCE);
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    @Test
    public void testConvertsToW3cDocumentFromByteArray() throws Exception
    {
        byte[] payload = XMLTestUtils.toString(SIMPLE_XML_RESOURCE).getBytes();
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    @Test
    public void testConvertsToW3cDocumentFromFile() throws Exception
    {
        URL asUrl = IOUtils.getResourceAsUrl(SIMPLE_XML_RESOURCE, getClass());
        File payload = new File(asUrl.getFile());
        assertToW3cDocumentSuccessfullyConvertsPayload(payload);
    }

    private void assertToW3cDocumentSuccessfullyConvertsPayload(Object payload) throws Exception
    {
        org.w3c.dom.Document document = XMLUtils.toW3cDocument(payload);
        String actualXml = XMLUtils.toXml(document);
        assertEquals(SIMPLE_XML_CONTENT, actualXml);
    }
}
