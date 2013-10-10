/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TransformerChainMule2063TestCase extends FunctionalTestCase
{

    public static final String IN = "in";
    public static final String TEST1_OUT = IN + "123";
    public static final String TEST2_OUT = IN + "123";
    public static final String TEST3_OUT = IN + "123abc";
    public static final long WAIT_MS = 3000L;

    @Override
    protected String getConfigResources()
    {
        return "issues/transformer-chain-mule-2063-test.xml";
    }

    protected void doTest(String name, String result) throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
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
