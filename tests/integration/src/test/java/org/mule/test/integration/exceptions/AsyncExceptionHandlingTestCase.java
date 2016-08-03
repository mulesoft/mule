/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertNotNull;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class AsyncExceptionHandlingTestCase extends AbstractIntegrationTestCase
{

    @Rule
    public DynamicPort dynamicPort1 = new DynamicPort("port1");
    @Rule
    public DynamicPort dynamicPort2 = new DynamicPort("port2");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/async-exception-handling-flow.xml";
    }

    @Test
    public void testAsyncExceptionHandlingTestCase() throws Exception
    {
        MuleClient client = muleContext.getClient();
        flowRunner("SearchWebServiceBridge").runExpectingException();
        assertNotNull(client.request("test://back-channel", RECEIVE_TIMEOUT));
    }
}
