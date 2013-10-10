/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * This test has been re-written to use entry point resolvers.
 */
public class NoArgsCallWrapperFunctionalTestCase extends AbstractServiceAndFlowTestCase
{
    private static final int RECEIVE_TIMEOUT = 5000;

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "no-args-call-wrapper-config-service.xml"},
            {ConfigVariant.FLOW, "no-args-call-wrapper-config-flow.xml"}});
    }

    public NoArgsCallWrapperFunctionalTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testNoArgsCallWrapper() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://invoke", "test", null);
        MuleMessage reply = client.request("vm://out", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        assertEquals("Just an apple.", reply.getPayload());
    }

    @Test
    public void testWithInjectedDelegate() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://invokeWithInjected", "test", null);
        MuleMessage reply = client.request("vm://outWithInjected", RECEIVE_TIMEOUT);
        assertNotNull(reply);
        assertNull(reply.getExceptionPayload());
        // same as original input
        assertEquals("test", reply.getPayload());
    }
}
