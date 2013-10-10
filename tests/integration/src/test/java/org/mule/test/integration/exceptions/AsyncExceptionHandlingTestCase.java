/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Map;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class AsyncExceptionHandlingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/async-exception-handling.xml";
    }

    @Test
    public void testAsyncExceptionHandlingTestCase() throws Exception
    {
        MuleClient client1 = new MuleClient(muleContext);
        DefaultMuleMessage msg1 = new DefaultMuleMessage("Hello World", (Map) null, muleContext);
        MuleMessage response1 = client1.send("search.inbound.endpoint", msg1, 300000);
        assertNotNull(response1);
    }

}
