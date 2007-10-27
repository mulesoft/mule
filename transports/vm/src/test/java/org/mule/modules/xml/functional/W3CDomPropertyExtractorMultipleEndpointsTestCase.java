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

import java.util.Properties;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class W3CDomPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public W3CDomPropertyExtractorMultipleEndpointsTestCase()
    {
        super(false);
    }

    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.property", "${xpath:/endpoints/endpoint}");
        return p;
    }

    protected Object getMatchMessage() throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        org.w3c.dom.Element endpoints = doc.createElement("endpoints");
        org.w3c.dom.Element endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode("matchingEndpoint1"));
        endpoints.appendChild(endpoint);
        endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode("matchingEndpoint2"));
        endpoints.appendChild(endpoint);
        doc.appendChild(endpoints);
        return doc;
    }

    protected Object getErrorMessage() throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        org.w3c.dom.Element endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode("missingEndpoint"));
        doc.appendChild(endpoint);
        return doc;
    }

    protected org.w3c.dom.Document documentFor(String name) throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        org.w3c.dom.Element endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode(name));
        doc.appendChild(endpoint);
        return doc;
    }

}