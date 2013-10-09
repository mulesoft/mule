/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.resolvers;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage response = client.send("vm://in", "payload", null);
        assertNotNull(response);
        assertNotNull(response.getPayload());
        assertEquals("Transformed payload", response.getPayloadAsString());
    }
}
