/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.functional.StringAppendTestTransformer;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class EndpointTransformerMule2131TestCase extends AbstractServiceAndFlowTestCase
{
    public static final String MESSAGE = "a message";

    public EndpointTransformerMule2131TestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
        setDisposeContextPerClass(true);
    }

    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "issues/endpoint-transformer-mule-2131-test-service.xml"},
            {ConfigVariant.FLOW, "issues/endpoint-transformer-mule-2131-test-flow.xml"}
        });
    }

    protected MuleClient send() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("in", MESSAGE, null);
        return client;
    }

    @Test
    public void testDirect() throws Exception
    {
        String response = request(send(), "direct");
        assertEquals(MESSAGE, response);
    }

    /* TODO This behaviour changed with BL-137, is this acceptable?
    @Test
    public void testGlobalNameGlobalTransformer() throws Exception
    {
        String response = request(send(), "global-name-global-transformer");
        // Transformer is applied twice because it is on global endpoint and is
        // therfore
        // used for both the inbound and outbound endpoints because both use the
        // global endpoint name and thus use the global endpoint as a template
        assertEquals(MESSAGE + StringAppendTestTransformer.DEFAULT_TEXT
                     + StringAppendTestTransformer.DEFAULT_TEXT, response);
    }
    */

    @Test
    public void testGlobalNameLocalTransformer() throws Exception
    {
        doTestTransformed("global-name-local-transformer");
    }

    @Test
    public void testNoNameLocalTransformer() throws Exception
    {
        doTestTransformed("vm://no-name-local-transformer");
    }

    @Test
    public void testLocalNameLocalTransformer() throws Exception
    {
        doTestTransformed("vm://local-name-local-transformer");
    }

    protected void doTestTransformed(String endpoint) throws Exception
    {
        String response = request(send(), endpoint);
        assertEquals(MESSAGE + StringAppendTestTransformer.DEFAULT_TEXT, response);
    }

    protected String request(MuleClient client, String endpoint) throws Exception
    {
        MuleMessage message = client.request(endpoint, RECEIVE_TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayloadAsString());
        return message.getPayloadAsString();
    }
}
