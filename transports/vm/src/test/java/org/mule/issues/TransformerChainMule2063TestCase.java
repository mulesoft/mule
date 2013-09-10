/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class TransformerChainMule2063TestCase extends AbstractServiceAndFlowTestCase
{
    public static final String IN = "in";
    public static final String TEST1_OUT = IN + "123";
    public static final String TEST2_OUT = IN + "123";
    public static final String TEST3_OUT = IN + "123abc";
    public static final long WAIT_MS = 3000L;

    public TransformerChainMule2063TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "issues/transformer-chain-mule-2063-test-service.xml"},
            {ConfigVariant.FLOW, "issues/transformer-chain-mule-2063-test-flow.xml"}});
    }

    protected void doTest(String name, String result) throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://" + name + "-in", IN, null);
        MuleMessage message = client.request("vm://" + name + "-out", WAIT_MS);

        assertNotNull(message);
        assertNotNull(message.getPayloadAsString());
        assertEquals(result, message.getPayloadAsString());
    }

    @Test
    public void testInputTransformers() throws Exception
    {
        doTest("test1", TEST1_OUT);
    }

    @Test
    public void testGlobalTransformers() throws Exception
    {
        doTest("test2", TEST2_OUT);
    }

    @Test
    public void testOutputTransformers() throws Exception
    {
        doTest("test3", TEST3_OUT);
    }
}
