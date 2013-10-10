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

public class W3CDomPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public W3CDomPropertyExtractorMultipleEndpointsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, false);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/endpoints/endpoint");
        p.setProperty("selector.evaluator", "xpath");

        return p;
    }

    @Override
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

    @Override
    protected Object getErrorMessage() throws ParserConfigurationException
    {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        org.w3c.dom.Document doc = builder.newDocument();
        org.w3c.dom.Element endpoint = doc.createElement("endpoint");
        endpoint.appendChild(doc.createTextNode("missingEndpoint"));
        doc.appendChild(endpoint);
        return doc;
    }
}
