/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.email.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class ExceptionHandlingMule2167TestCase extends FunctionalTestCase
{
    public static final String MESSAGE = "a message";
    public static final long WAIT_MS = 3000L;

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigFile()
    {
        return "exception-handling-mule-2167-test.xml";
    }

    @Test
    public void testDefaultConfig() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in-default", MESSAGE, null);

        MuleMessage message = client.request("vm://out-default", WAIT_MS);
        assertNotNull("null message", message);
        assertNotNull("null payload", message.getPayload());
        assertEquals(MESSAGE, message.getPayloadAsString());
    }
}
