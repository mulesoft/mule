/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExceptionBasedRouterTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{{ConfigVariant.SERVICE,
            "org/mule/test/integration/routing/outbound/exception-based-router-service.xml"},});
    }

    public ExceptionBasedRouterTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testStaticEndpointsByName() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage reply = client.send("vm://in1", "request", null);
        assertNotNull(reply);
        assertEquals("success", reply.getPayload());
    }

    @Test
    public void testStaticEndpointsByURI() throws Exception
    {
        MuleClient client = muleContext.getClient();

        MuleMessage reply = client.send("vm://in2", "request", null);
        assertNotNull(reply);
        assertEquals("success", reply.getPayload());
    }

    @Test
    public void testDynamicEndpointsByName() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        props.put("recipients", "service1,service2,service3");
        MuleMessage reply = client.send("vm://in3", "request", props);
        assertNotNull(reply);
        assertEquals("success", reply.getPayload());
    }

    @Test
    public void testDynamicEndpointsByURI() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        List<String> recipients = new ArrayList<String>();
        recipients.add("vm://service4?responseTransformers=validateResponse&exchangePattern=request-response");
        recipients.add("vm://service5?responseTransformers=validateResponse&exchangePattern=request-response");
        recipients.add("vm://service6?responseTransformers=validateResponse&exchangePattern=request-response");
        props.put("recipients", recipients);
        MuleMessage reply = client.send("vm://in3", "request", props);
        assertNotNull(reply);
        assertEquals("success", reply.getPayload());
    }

    /**
     * Test endpoints which generate a natural exception because they don't even
     * exist.
     */
    @Test
    public void testIllegalEndpoint() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> props = new HashMap<String, Object>();
        List<String> recipients = new ArrayList<String>();
        recipients.add("vm://service998?exchangePattern=request-response");
        recipients.add("vm://service5?exchangePattern=request-response");
        recipients.add("vm://service999");
        props.put("recipients", recipients);
        MuleMessage reply = client.send("vm://in3", "request", props);
        assertNotNull(reply);
        assertEquals("success", reply.getPayload());
    }

    /**
     * Test failing endpoint do not cause transaction rollback
     */
    @Test
    public void testTransactionIsNotRolledBack() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage result = client.send("jms://in", "some message", null, RECEIVE_TIMEOUT);
        assertThat(result, IsNull.<Object>notNullValue());
        assertThat((NullPayload) result.getPayload(), is(NullPayload.getInstance()));
        assertThat(result.getExceptionPayload(), IsNull.<Object>nullValue());
        MuleMessage outputMessage = client.request("jms://out",RECEIVE_TIMEOUT);
        assertThat(outputMessage, IsNull.<Object>notNullValue());
        assertThat(outputMessage.getPayloadAsString(), is("some message"));
    }
}
