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

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.providers.NullPayload;
import org.mule.providers.http.HttpConnector;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOMessage;

public class HttpResponseTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/http/http-response.xml";
    }

    public void testNullPayloadUsingAsync() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("http://localhost:8990", new MuleMessage("test"));

        //TODO RM: What should really be returned when doing an async request?
        assertNotNull(reply.getPayload());
        assertEquals(reply.getIntProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0), 200);
        assertEquals(0, reply.getPayloadAsString().length());
    }

    public void testPayloadIsNotEmptyNoRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("http://localhost:8999", new MuleMessage("test"));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }

    public void testPayloadIsNotEmptyWithRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient();
        UMOMessage reply = client.send("http://localhost:8989", new MuleMessage("test"));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }
}
