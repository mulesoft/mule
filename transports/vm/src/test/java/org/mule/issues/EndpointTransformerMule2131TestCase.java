/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.issues;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.tck.functional.StringAppendTestTransformer;

public class EndpointTransformerMule2131TestCase extends FunctionalTestCase
{

    public static final long TIMEOUT = 1000L;
    public static final String MESSAGE = "a message";

    protected String getConfigResources()
    {
        return "issues/endpoint-transformer-mule-2131-test.xml";
    }

    protected MuleClient send() throws MuleException
    {
        MuleClient client = new MuleClient();
        client.dispatch("in", MESSAGE, null);
        return client;
    }

    public void testDirect() throws Exception
    {
        String response = request(send(), "direct");
        assertEquals(MESSAGE, response);
    }

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

    public void testGlobalNameLocalTransformer() throws Exception
    {
        doTestTransformed("global-name-local-transformer");
    }

    public void testNoNameLocalTransformer() throws Exception
    {
        doTestTransformed("vm://no-name-local-transformer?connector=queue");
    }

    public void testLocalNameLocalTransformer() throws Exception
    {
        doTestTransformed("vm://local-name-local-transformer?connector=queue");
    }

    protected void doTestTransformed(String endpoint) throws Exception
    {
        String response = request(send(), endpoint);
        assertEquals(MESSAGE + StringAppendTestTransformer.DEFAULT_TEXT, response);
    }

    protected String request(MuleClient client, String endpoint) throws Exception
    {
        MuleMessage message = client.request(endpoint, TIMEOUT);
        assertNotNull(message);
        assertNotNull(message.getPayloadAsString());
        return message.getPayloadAsString();
    }

}
