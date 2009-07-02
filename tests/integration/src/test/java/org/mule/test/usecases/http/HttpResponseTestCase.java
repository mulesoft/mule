/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.usecases.http;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.NullPayload;
import org.mule.transport.http.HttpConnector;

public class HttpResponseTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/http/http-response.xml";
    }

    public void testNullPayloadUsingAsync() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("http://localhost:8990", new DefaultMuleMessage("test", muleContext));

        //TODO RM: What should really be returned when doing an async request?
        assertNotNull(reply.getPayload());
        assertEquals(reply.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0), 200);
        assertEquals(0, reply.getPayloadAsString().length());
    }

    public void testPayloadIsNotEmptyNoRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("http://localhost:8999", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }

    public void testPayloadIsNotEmptyWithRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient();
        MuleMessage reply = client.send("http://localhost:8989", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }
}
