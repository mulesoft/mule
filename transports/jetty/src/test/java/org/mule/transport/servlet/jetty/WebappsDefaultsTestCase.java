/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;

public class WebappsDefaultsTestCase extends AbstractWebappsTestCase
{
    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "jetty-webapps-with-defaults.xml";
    }

    @Test
    public void webappWithDefaultsShouldBeDeployed() throws Exception
    {
        sendRequestAndAssertCorrectResponse(String.format("http://localhost:%s/test/hello",dynamicPort.getNumber()));
    }
}
