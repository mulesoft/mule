/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.resolvers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class MethodEntryPointWithTransformerTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/resolvers/method-entrypoint-with-transformer-config-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/resolvers/method-entrypoint-with-transformer-config-flow.xml"}});
    }

    public MethodEntryPointWithTransformerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    /**
     * Tests that a MethodEntryPointResolver is able to receive the method property
     * from a MessagePropertyTransformer, that means that the transformer is applied
     * before resolving that property.
     */
    @Test
    public void testReceivesMethodPropertyFromAPropertyTransformer() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://in", "payload", null);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Transformed payload", response.getPayloadAsString());
    }
}
