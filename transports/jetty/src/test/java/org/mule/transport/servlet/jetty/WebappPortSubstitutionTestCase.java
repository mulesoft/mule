/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
