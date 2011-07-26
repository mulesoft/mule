/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
