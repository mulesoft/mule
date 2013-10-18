/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
