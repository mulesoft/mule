/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.util;

import org.mule.util.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class XMLTestUtils
{

    public static List<?> getXmlMessageVariants(String resource) throws Exception
    {
        List<Object> list = new ArrayList<Object>();

        list.add(toInputStream(resource));
        list.add(toDom4jDocument(resource));
        list.add(toW3cDocument(resource));
        list.add(toInputSource(resource));
        list.add(toSource(resource));
        list.add(toXmlStreamReader(resource));

        return list;
    }

    public static XMLStreamReader toXmlStreamReader(String resource)
            throws IOException, XMLStreamException
    {
        InputStream is = toInputStream(resource);

        return XMLUtils.toXMLStreamReader(XMLInputFactory.newInstance(), is);
    }

    public static Source toSource(String resource) throws Exception
    {
        InputStream is = toInputStream(resource);

        return XMLUtils.toXmlSource(XMLInputFactory.newInstance(), false, is);
    }

    public static org.w3c.dom.Document toW3cDocument(String resource) throws IOException, SAXException, ParserConfigurationException
    {
        InputStream is = toInputStream(resource);

        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
    }

    public static InputSource toInputSource(String resource) throws IOException
    {
        InputStream is = toInputStream(resource);

        return new InputSource(is);
    }

    public static Document toDom4jDocument(String resource) throws IOException, DocumentException
    {
        String xml = toString(resource);
        return DocumentHelper.parseText(xml);
    }

    public static String toString(String resource) throws IOException
    {
        return IOUtils.getResourceAsString(resource, XMLTestUtils.class);
    }

    public static InputStream toInputStream(String resource) throws IOException
    {
        return IOUtils.getResourceAsStream(resource, XMLTestUtils.class);
    }
}
