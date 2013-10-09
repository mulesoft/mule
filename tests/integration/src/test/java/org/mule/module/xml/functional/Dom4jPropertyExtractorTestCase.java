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

public class Dom4jPropertyExtractorTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public Dom4jPropertyExtractorTestCase(ConfigVariant variant, String configResources)
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
    protected Object getMatchMessage()
    {
        Document document = DocumentHelper.createDocument();
        document.addElement("endpoint").addText("matchingEndpoint1");
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
