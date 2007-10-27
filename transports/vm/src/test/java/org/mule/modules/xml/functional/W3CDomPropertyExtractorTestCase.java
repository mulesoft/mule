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

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class W3CDomPropertyExtractorTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public W3CDomPropertyExtractorTestCase()
    {
        super(true);
    }

    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.property", "${xpath:/endpoint}");
        return p;
    }

    protected Object getMatchMessage() throws ParserConfigurationException
    {
        return documentFor("matchingEndpoint1");
    }

    protected Object getErrorMessage() throws ParserConfigurationException
    {
        return documentFor("missingEndpoint");
    }

    protected Document documentFor(String name) throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.newDocument();
        Element endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode(name));
        doc.appendChild(endpoint);
        return doc;
    }

}
