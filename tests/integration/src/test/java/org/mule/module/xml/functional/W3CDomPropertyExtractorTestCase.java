/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class W3CDomPropertyExtractorTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public W3CDomPropertyExtractorTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, true);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/endpoint");
        p.setProperty("selector.evaluator", "xpath");

        return p;
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
