/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class AsyncExceptionHandlingTestCase extends AbstractServiceAndFlowTestCase
{
    String request = "Hello World";

    public AsyncExceptionHandlingTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/async-exception-handling-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/async-exception-handling-flow.xml"}
        });
    }

    @Test
    public void testAsyncExceptionHandlingTestCase() throws Exception
    {
        MuleClient client1 = new MuleClient(muleContext);
        DefaultMuleMessage msg1 = new DefaultMuleMessage(request, (Map) null, muleContext);
        MuleMessage response1 = client1.send("search.inbound.endpoint", msg1, 300000);
        assertNotNull(response1);
    }

}
