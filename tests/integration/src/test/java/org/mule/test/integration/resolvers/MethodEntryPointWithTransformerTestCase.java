/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class MethodEntryPointWithTransformerTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/resolvers/method-entrypoint-with-transformer-config.xml";
    }

    /**
     * Tests that a MethodEntryPointResolver is able to receive the method property
     * from a MessagePropertyTransformer, that means that the transformer is applied
     * before resolving that property.
     */
    @Test
    public void testReceivesMethodPropertyFromAPropertyTransformer() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", "payload", null);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Transformed payload", response.getPayloadAsString());
    }

    /**
     * Transforms a message for testing purposes. 
     * <p> Is referenced by the test configuration because it implements the test component method which should be call by the MethodEntryPointResolver.
     */
    public String transformMessage(String message)
    {
        return "Transformed " + message;
    }
}
