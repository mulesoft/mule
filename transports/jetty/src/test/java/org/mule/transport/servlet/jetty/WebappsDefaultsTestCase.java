/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.junit.Test;

public class WebappsDefaultsTestCase extends AbstractWebappsTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jetty-webapps-with-defaults.xml";
    }

    @Test
    public void webappWithDefaultsShouldBeDeployed() throws Exception
    {
        sendRequestAndAssertCorrectResponse("http://localhost:8585/test/hello");
    }
}
