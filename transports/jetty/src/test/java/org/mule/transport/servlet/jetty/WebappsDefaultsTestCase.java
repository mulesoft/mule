/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.servlet.jetty;

import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WebappsDefaultsTestCase extends AbstractWebappsTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "jetty-webapps-with-defaults.xml";
    }

    @Test
    public void webappWithDefaultsShouldBeDeployed() throws Exception
    {
        sendRequestAndAssertCorrectResponse(String.format("http://localhost:%s/%s", dynamicPort.getNumber(), WEBAPP_TEST_URL));
    }
}
