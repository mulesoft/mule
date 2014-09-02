/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jersey;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.http.HttpConnector;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.junit.Test;

public class JerseyStreamingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "jersey-streaming-config.xml";
    }

    @Test
    public void consumesJerseyStreamingOutputWhenOutputHandlerIsConsumed() throws Exception
    {
        MuleClient client = muleContext.getClient();

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(HttpConnector.HTTP_REQUEST_PROPERTY, "/resource/operation");
        properties.put(HttpConnector.HTTP_CONTEXT_PATH_PROPERTY, "");
        properties.put(HttpConnector.HTTP_METHOD_PROPERTY, "GET");

        client.dispatch("vm://in", null, properties);
        MuleMessage response = client.request("vm://out", RECEIVE_TIMEOUT);

        TestResource testResource = muleContext.getRegistry().get("testResourceBean");

        assertThat(testResource.isCalled(), is(true));
        assertThat(testResource.getTestStreamingOutput().isCalled(), is(false));

        response.getPayloadAsBytes();

        assertThat(testResource.getTestStreamingOutput().isCalled(), is(true));
    }

    @Path("/resource")
    public static class TestResource
    {
        private volatile boolean called = false;
        private TestStreamingOutput testStreamingOutput = new TestStreamingOutput();

        @GET
        @Path("/operation")
        public Response testOperation() {
            called = true;
            return Response.ok(testStreamingOutput).build();
        }

        public TestStreamingOutput getTestStreamingOutput()
        {
            return testStreamingOutput;
        }

        public boolean isCalled()
        {
            return called;
        }
    }


    public static class TestStreamingOutput implements StreamingOutput
    {
        private volatile boolean called = false;

        @Override
        public void write(OutputStream output) throws IOException, WebApplicationException
        {
            called = true;
            output.write(TEST_MESSAGE.getBytes());
        }

        public boolean isCalled()
        {
            return called;
        }
    }
}
