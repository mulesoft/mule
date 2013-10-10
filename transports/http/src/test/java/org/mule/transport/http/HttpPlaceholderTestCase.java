/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.http;

import org.junit.Test;

public class HttpPlaceholderTestCase extends AbstractNamespaceHandlerTestCase
{

    public HttpPlaceholderTestCase()
    {
        super("http");
    }

    @Override
    protected String getConfigResources()
    {
        return "http-placeholder-test.xml";
    }

    @Test
    public void testConnectorProperties()
    {
        HttpConnector connector =
                (HttpConnector) muleContext.getRegistry().lookupConnector("httpConnector");
        testBasicProperties(connector);
    }

}
