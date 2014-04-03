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

public class SslWebappsTestCase extends AbstractWebappsTestCase
{
    @Rule
    public DynamicPort httpPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "jetty-ssl-webapps.xml";
    }

    @Test
    public void webappShouldBeDeployed() throws Exception
    {
        System.out.println(baseDirProperty.getValue());
        sendRequestAndAssertCorrectResponse(String.format("https://localhost:%d/%s", httpPort.getNumber(), WEBAPP_TEST_URL));
    }
}
