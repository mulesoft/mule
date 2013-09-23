/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.usecases.http;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

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
        MuleClient client = new MuleClient(muleContext);
        MuleMessage message = client.send("httpRequest", "payload", null);
        assertNotNull(message);
        assertNotNull(message.getPayloadAsString());
        assertEquals("IncidentData=payload", message.getPayloadAsString());
    }
}
