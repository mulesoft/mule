/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.springconfig;

import org.mule.api.MuleException;
import org.mule.api.client.MuleClient;

import org.junit.Test;

public class NamedInterceptorTestCase extends AbstractInterceptorTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/config/spring/named-interceptor-test-flow.xml";
    }

    @Test
    public void testInterceptor() throws MuleException, InterruptedException
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://in", MESSAGE, null);
        assertMessageIntercepted();
    }
}
