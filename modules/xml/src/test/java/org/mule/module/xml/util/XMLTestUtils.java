/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.xml.util;

import org.mule.util.IOUtils;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;

import org.dom4j.DocumentHelper;
import org.xml.sax.InputSource;

public class XMLTestUtils
{
    public static List<?> getXmlMessageVariants(String resource) throws Exception
    {
        XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
        
        List<Object> list = new ArrayList<Object>();
        InputStream is;

        // java.io.InputStream
        is = IOUtils.getResourceAsStream(resource, XMLTestUtils.class);
        list.add(is);

        // org.dom4j.Document
        String xml = IOUtils.getResourceAsString(resource, XMLTestUtils.class);
        org.dom4j.Document dom4jDoc = DocumentHelper.parseText(xml);
        list.add(dom4jDoc);

        // org.w3c.dom.Document
        is = IOUtils.getResourceAsStream(resource, XMLTestUtils.class);
        org.w3c.dom.Document w3cDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(is);
        list.add(w3cDoc);
        
        // org.xml.sax.InputSource
        is = IOUtils.getResourceAsStream(resource, XMLTestUtils.class);
        list.add(new InputSource(is));
        
        // javax.xml.transform.Source
        is = IOUtils.getResourceAsStream(resource, XMLTestUtils.class);
        Source s = XMLUtils.toXmlSource(xmlInputFactory, false, is);
        list.add(s);
        
        // javax.xml.stream.XMLStreamReader
        is = IOUtils.getResourceAsStream(resource, XMLTestUtils.class);
        XMLStreamReader sr = XMLUtils.toXMLStreamReader(XMLInputFactory.newInstance(), is);
        list.add(sr);

        return list;
    }
}
