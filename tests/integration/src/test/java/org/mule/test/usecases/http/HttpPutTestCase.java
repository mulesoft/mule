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
import org.mule.api.transport.PropertyScope;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;

import org.junit.Rule;
import org.junit.Test;

public class HttpPutTestCase extends FunctionalTestCase
{

    @Rule
    public DynamicPort dynamicPort = new DynamicPort("port1");

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/http/http-put-config.xml";
    }

    @Test
    public void testPreservesContentType() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);

        MuleMessage message = client.send("httpRequest", "TEST", null);

        assertNotNull(message);
        assertEquals("TEST", message.getPayloadAsString());
        assertEquals("application/xml", message.getProperty("Content-type", PropertyScope.INBOUND));
    }
}
