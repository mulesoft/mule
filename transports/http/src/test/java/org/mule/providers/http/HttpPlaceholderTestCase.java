/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.http;

public class HttpPlaceholderTestCase extends AbstractNamespaceHandlerTestCase
{

    public HttpPlaceholderTestCase()
    {
        super("http");
    }

    protected String getConfigResources()
    {
        return "http-placeholder-test.xml";
    }

    public void testConnectorProperties()
    {
        HttpConnector connector =
                (HttpConnector) managementContext.getRegistry().lookupConnector("httpConnector");
        testBasicProperties(connector);
    }

}