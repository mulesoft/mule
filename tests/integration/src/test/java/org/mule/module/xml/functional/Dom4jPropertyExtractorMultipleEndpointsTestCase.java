/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import java.util.Properties;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

public class Dom4jPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public Dom4jPropertyExtractorMultipleEndpointsTestCase(ConfigVariant variant, String configResources)
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
    protected Object getMatchMessage()
    {
        Document document = DocumentHelper.createDocument();
        Element e = document.addElement("endpoints");
        e.addElement("endpoint").addText("matchingEndpoint1");
        e.addElement("endpoint").addText("matchingEndpoint2");
        return document;
    }

    @Override
    protected Object getErrorMessage()
    {
        Document document = DocumentHelper.createDocument();
        document.addElement("endpoint").addText("missingEndpoint");
        return document;
    }

}
