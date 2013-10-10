/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.routing.response;


import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
/**
 * Test the request-reply construct in flows
 */
public class RequestReplyInFlowTestCase extends AbstractServiceAndFlowTestCase
{
        @Parameterized.Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.FLOW, "org/mule/test/usecases/routing/response/request-reply-flow.xml"}});
    }

    public RequestReplyInFlowTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testRequestReply() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("vm://input", "Message went", null);
        MuleMessage reply = client.request("vm://destination", 10000);
        assertNotNull(reply);
        assertEquals("Message went-out-and-back-in", reply.getPayload());
    }
}
