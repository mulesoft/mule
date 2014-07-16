/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;
import org.mule.transport.http.HttpConstants;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class HttpResponseTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/usecases/http/http-response-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/usecases/http/http-response-flow.xml"}
        });
    }

    public HttpResponseTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testNullPayloadUsingAsync() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("http://localhost:8990", new DefaultMuleMessage("test", muleContext));

        //TODO RM: What should really be returned when doing an async request?
        assertNotNull(reply.getPayload());
        int status = reply.getInboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(status, 200);
        assertEquals(0, reply.getPayloadAsString().length());
    }

    @Test
    public void testPayloadIsNotEmptyNoRemoteSynch() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("http://localhost:8999", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }

    @Test
    public void testPayloadIsNotEmptyWithRemoteSynch() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("http://localhost:8989", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }

    /**
     * See MULE-4522
     * @throws Exception
     */
    @Test
    public void testChunkingContentLength() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("http://localhost:8988", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("chunked", reply.getInboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING));
        assertNull(reply.getInboundProperty(HttpConstants.HEADER_CONTENT_LENGTH));
    }

    @Test
    public void testNoChunkingContentLength() throws Exception
    {
        MuleClient client = muleContext.getClient();
        MuleMessage reply = client.send("http://localhost:8987", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertNotSame("chunked", reply.getInboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING));
        assertNotNull(reply.getInboundProperty(HttpConstants.HEADER_CONTENT_LENGTH));
    }
}
