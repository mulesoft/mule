/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class WebappPortSubstitutionTestCase extends AbstractWebappsTestCase
{
    @Rule
    public DynamicPort port1 = new DynamicPort("webapp-port");

    @Override
    protected String getConfigResources()
    {
        return "jetty-port-substitution-webapps.xml";
    }

    @Test
    public void webappShouldRunOnDynamicPort() throws Exception
    {
        String url = String.format("http://localhost:%d/test/hello", port1.getNumber());
        sendRequestAndAssertCorrectResponse(url);
    }
}
