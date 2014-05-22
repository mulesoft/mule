/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.message;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Rule;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.NonSerializableObject;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;

/**
 * see EE-2307
 *
 * Non serializable session properties won't be serialized with the mule session
 * but neither it will be lost in the flow were it was stored
 */
public class SessionPropertyChainingRouterTestCase extends FunctionalTestCase
{
    @Rule
    public DynamicPort serverPort = new DynamicPort("serverPort");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/session-property-chaining-router.xml";
    }

    @Test
    public void testRouter() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "test message", null);
        assertNotNull(response);
        assertTrue("Response is " + response.getPayload(), response.getPayload() instanceof NonSerializableObject);
    }
}


