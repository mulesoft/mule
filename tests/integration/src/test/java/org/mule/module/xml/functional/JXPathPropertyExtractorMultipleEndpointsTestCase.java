/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.xml.functional;

import java.util.Properties;

public class JXPathPropertyExtractorMultipleEndpointsTestCase extends AbstractXmlPropertyExtractorTestCase
{

    public JXPathPropertyExtractorMultipleEndpointsTestCase()
    {
        super(true);
    }

    @Override
    protected Properties getStartUpProperties()
    {
        Properties p = new Properties();
        p.setProperty("selector.expression", "jxpath:/endpoints/endpoint");
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
