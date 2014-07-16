/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpPostTestCase extends AbstractServiceAndFlowTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("port");

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/usecases/http/http-post-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/usecases/http/http-post-flow.xml"}
        });
    }

    public HttpPostTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testPost() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage message = client.send("httpRequest", "payload", null);
        assertNotNull(message);
        assertNotNull(message.getPayloadAsString());
        assertEquals("IncidentData=payload", message.getPayloadAsString());
    }
}
