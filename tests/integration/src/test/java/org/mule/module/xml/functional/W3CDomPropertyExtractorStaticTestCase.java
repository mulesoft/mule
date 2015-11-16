/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class W3CDomPropertyExtractorStaticTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public W3CDomPropertyExtractorStaticTestCase()
    {
        super(true);
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/module/xml/property-extractor-static-test-flow.xml";
    }

    @Override
    protected Object getMatchMessage() throws ParserConfigurationException
    {
        return documentFor("matchingEndpoint1");
    }

    @Override
    protected Object getErrorMessage() throws ParserConfigurationException
    {
        return documentFor("missingEndpoint");
    }

    protected Document documentFor(String nodeName) throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode(nodeName));
        doc.appendChild(endpoint);
        return doc;
    }
}
