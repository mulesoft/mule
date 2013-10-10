/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.servlet.jetty;

import org.junit.Test;

public class WebappsTestCase extends AbstractWebappsTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jetty-webapps.xml";
    }

    @Test
    public void webappShouldBeDeployed() throws Exception
    {
        sendRequestAndAssertCorrectResponse("http://localhost:63081/test/hello");
    }
}
