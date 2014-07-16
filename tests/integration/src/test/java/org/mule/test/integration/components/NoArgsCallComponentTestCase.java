/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.components;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * This test re-written to use entry point resolvers.  As a consequence, some tests, which verified
 * implementation details rather than functionality, were dropped.
 */
public class NoArgsCallComponentTestCase extends AbstractServiceAndFlowTestCase
{
    public static final String INPUT_DC_QUEUE_NAME = "vm://in";
    public static final String OUTPUT_DC_QUEUE_NAME = "vm://out";
    public static final String INPUT_DI_QUEUE_NAME = "vm://invokeWithInjected";
    public static final String OUTPUT_DI_QUEUE_NAME = "vm://outWithInjected";

    public static final String DEFAULT_INPUT_MESSAGE = "test";
    public static final String DEFUALT_OUTPUT_MESSAGE = "Just an apple.";

    public static final int TIMEOUT = 5000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/components/no-args-call-component-functional-test-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/components/no-args-call-component-functional-test-flow.xml"}
        });
    }

    public NoArgsCallComponentTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDelegateClass() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch(INPUT_DC_QUEUE_NAME, "test", null);
        MuleMessage message = client.request(OUTPUT_DC_QUEUE_NAME, TIMEOUT);
        assertNotNull(message);
        assertEquals(message.getPayload(), DEFUALT_OUTPUT_MESSAGE);
    }

    @Test
    public void testWithInjectedDelegate() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.dispatch(INPUT_DI_QUEUE_NAME, DEFAULT_INPUT_MESSAGE, null);
        MuleMessage reply = client.request(OUTPUT_DI_QUEUE_NAME, TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        // same as original input
        assertEquals(DEFAULT_INPUT_MESSAGE, reply.getPayload());
    }
}
