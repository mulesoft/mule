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

import org.junit.Test;

public class SslWebappsTestCase extends AbstractWebappsTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "jetty-ssl-webapps.xml";
    }

    @Test
    public void webappShouldBeDeployed() throws Exception
    {
        sendRequestAndAssertCorrectResponse("https://localhost:63083/test/hello");
    }
}
