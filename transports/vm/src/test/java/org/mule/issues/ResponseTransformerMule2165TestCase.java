/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.client.MuleClient;

import org.junit.Test;

public class ResponseTransformerMule2165TestCase extends FunctionalTestCase
{

    public static final long TIMEOUT = 2000L;
    public static final String MESSAGE = "a message";
    // i don't know if this is the "correct" response - it's *one* of the responses in 1.4,
    // and it seems vaguely reasonable.

    /* RM
    Described as:
    1. Client dispatch = "outbound"
    2. First Service receiver = "inbound"
    3. First Service dispatch (first endpoint) = "outbound"
    4. Second Service receiver = "inbound"
    5. Response transformer from SecondComponent = "response"
    Note that because the response transformer is configured locally on the outbound endppoint it only gets called once
    */
    public static final String LOCAL_RESPONSE = MESSAGE + " outbound inbound outbound inbound response";
    // an alternative in 1.4 is " outbound outbound response response" for the global
    // transformers, which also makes some sense
    /* RM
    Described as:
    1. Client dispatch = "outbound"
    2. First Service receiver = "inbound"
    3. First Service dispatch (first endpoint) = "outbound"
    4. Second Service receiver = "inbound"
    5. Response transformer from SecondComponent = "response"
    6. Response from outbound endpoint (to the service) = "response"
    Note that because the global outbound inpoint is also the inbound endpoint of the bounce service
    The "response" ResponseTransformer gets called twice
    */
    public static final String GLOBAL_RESPONSE = LOCAL_RESPONSE + " response";

    @Override
    protected String getConfigFile()
    {
        return "issues/response-transformer-mule-2165-test-flow.xml";
    }

    protected MuleClient send(String endpoint) throws MuleException
    {
        MuleClient client = muleContext.getClient();
        client.dispatch(endpoint, MESSAGE, null);
        return client;
    }

    protected MuleClient dispatch(String endpoint) throws MuleException
    {
        MuleClient client = muleContext.getClient();
        client.dispatch(endpoint, MESSAGE, null);
        return client;
    }

    protected String request(MuleClient client, String endpoint) throws Exception
    {
        MuleMessage message = client.request(endpoint, TIMEOUT);
        assertNotNull("no response from " + endpoint, message);
        assertNotNull(getPayloadAsString(message));
        return getPayloadAsString(message);
    }

    protected void testTransformered(String endpoint, String response) throws Exception
    {
        String message = request(send("in-" + endpoint), "out-" + endpoint);
        assertEquals("bad response (" + message + ")  for " + endpoint, response, message);
    }

    protected void testTransformeredAsync(String endpoint, String response) throws Exception
    {
        String message = request(dispatch("in-" + endpoint), "out-" + endpoint);
        assertEquals("bad response (" + message + ")  for " + endpoint, response, message);
    }

    @Test
    public void testGlobalNameGlobalTransformer() throws Exception
    {
        testTransformered("global-name-global-transformer", GLOBAL_RESPONSE);
    }

    @Test
    public void testGlobalNameUrlTransformer() throws Exception
    {
        testTransformered("global-name-url-transformer", GLOBAL_RESPONSE);
    }

    @Test
    public void testGlobalNameLocalTransformer() throws Exception
    {
        testTransformered("global-name-local-transformer", LOCAL_RESPONSE);
    }

    @Test
    public void testLocalNameLocalTransformer() throws Exception
    {
        testTransformered("local-name-local-transformer", LOCAL_RESPONSE);
    }

    @Test
    public void testLocalNameUrlTransformer() throws Exception
    {
        testTransformered("local-name-url-transformer", LOCAL_RESPONSE);
    }


    @Test
    public void testGlobalNameGlobalTransformerAsync() throws Exception
    {
        testTransformeredAsync("global-name-global-transformer", GLOBAL_RESPONSE);
    }

    @Test
    public void testGlobalNameUrlTransformerAsync() throws Exception
    {
        testTransformeredAsync("global-name-url-transformer", GLOBAL_RESPONSE);
    }

    @Test
    public void testGlobalNameLocalTransformerAsync() throws Exception
    {
        testTransformeredAsync("global-name-local-transformer", LOCAL_RESPONSE);
    }

    @Test
    public void testLocalNameLocalTransformerAsync() throws Exception
    {
        testTransformeredAsync("local-name-local-transformer", LOCAL_RESPONSE);
    }

    @Test
    public void testLocalNameUrlTransformerAsync() throws Exception
    {
        testTransformeredAsync("local-name-url-transformer", LOCAL_RESPONSE);
    }
}
