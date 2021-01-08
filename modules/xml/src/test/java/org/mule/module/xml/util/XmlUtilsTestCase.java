/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.mule.module.xml.util.XMLUtils.*;
import static org.mule.util.xmlsecurity.XMLSecureFactories.*;

import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.IOUtils;

import java.io.ByteArrayInputStream;
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

    @Test
    public void testConvertsToNullWhenXmlResourceIsEmpty() throws Exception
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://inbound.tpmtogglevalues.cocacola.com/\"><SOAP-ENV:Header/><SOAP-ENV:Body></SOAP-ENV:Body></SOAP-ENV:Envelope>".getBytes());
        XMLStreamReader xmlStreamReader = createDefault().getXMLInputFactory().createXMLStreamReader(inputStream);
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        Source source = toXmlSource(xmlStreamReader); // Here I am at the end of the soap body tag / beginning of closing envelope tag
        assertThat(source, nullValue());
    }

    @Test
    public void testConvertsWhenXmlResourceIsNotEmpty() throws Exception
    {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:tns=\"http://inbound.tpmtogglevalues.cocacola.com/\"><SOAP-ENV:Header/><SOAP-ENV:Body><test>somecontent</test></SOAP-ENV:Body></SOAP-ENV:Envelope>".getBytes());
        XMLStreamReader xmlStreamReader = createDefault().getXMLInputFactory().createXMLStreamReader(inputStream);
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        xmlStreamReader.nextTag();
        Source source = toXmlSource(xmlStreamReader); // Here, I am at the text element inside the test tag
        assertThat(source, notNullValue());
    }

    private void assertToW3cDocumentSuccessfullyConvertsPayload(Object payload) throws Exception
    {
        org.w3c.dom.Document document = toW3cDocument(payload);
        String actualXml = toXml(document);
        assertEquals(SIMPLE_XML_CONTENT, actualXml);
    }
}
