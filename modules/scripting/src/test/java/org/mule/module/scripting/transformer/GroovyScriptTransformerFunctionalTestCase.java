/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.module.scripting.transformer;

import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

public class GroovyScriptTransformerFunctionalTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "groovy-transformer-config.xml";
    }

    public void testInlineScript() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in1", "hello", null);
        MuleMessage response = client.request("vm://out1", 1000);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    public void testFileBasedScript() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in2", "hello", null);
        MuleMessage response = client.request("vm://out2", 1000);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    public void testReferencedTransformer() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in3", "hello", null);
        MuleMessage response = client.request("vm://out3", 1000);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }

    public void testReferencedTransformerWithParameters() throws Exception
    {
        MuleClient client = new MuleClient();
        client.send("vm://in4", "hello", null);
        MuleMessage response = client.request("vm://out4", 1000);
        assertNotNull(response);
        assertEquals("hexxo", response.getPayload());
    }
}
