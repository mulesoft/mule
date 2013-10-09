/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.xml.functional;

import java.util.Properties;

public class JXPathPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public JXPathPropertyExtractorMultipleEndpointsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources, true);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "/endpoints/endpoint");
        p.setProperty("selector.evaluator", "jxpath");

        return p;
    }

    @Override
    protected Object getMatchMessage()
    {
        return "<endpoints><endpoint>matchingEndpoint1</endpoint><endpoint>matchingEndpoint2</endpoint></endpoints>";
    }

    @Override
    protected Object getErrorMessage()
    {
        return "<endpoint>missingEndpoint</endpoint>";
    }

}
