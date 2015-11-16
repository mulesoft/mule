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
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpPostTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort httpPort = new DynamicPort("port");

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/usecases/http/http-post-flow.xml";
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
