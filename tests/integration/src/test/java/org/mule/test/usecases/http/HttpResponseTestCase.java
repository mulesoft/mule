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
import org.mule.transport.http.HttpConstants;

public class HttpResponseTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "org/mule/test/usecases/http/http-response.xml";
    }

    public void testNullPayloadUsingAsync() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("http://localhost:8990", new DefaultMuleMessage("test", muleContext));

        //TODO RM: What should really be returned when doing an async request?
        assertNotNull(reply.getPayload());
        int status = reply.getOutboundProperty(HttpConnector.HTTP_STATUS_PROPERTY, 0);
        assertEquals(status, 200);
        assertEquals(0, reply.getPayloadAsString().length());
    }

    public void testPayloadIsNotEmptyNoRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("http://localhost:8999", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }

    public void testPayloadIsNotEmptyWithRemoteSynch() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("http://localhost:8989", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("test", reply.getPayloadAsString());
    }
    
    /**
     * See MULE-4522
     * @throws Exception
     */
    public void testChunkingContentLength() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("http://localhost:8988", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertEquals("chunked", reply.getOutboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING));
        assertNull(reply.getOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH));
    }

    public void testNoChunkingContentLength() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        MuleMessage reply = client.send("http://localhost:8987", new DefaultMuleMessage("test", muleContext));
        assertNotNull(reply.getPayload());
        assertFalse(reply.getPayload() instanceof NullPayload);
        assertNotSame("chunked", reply.getOutboundProperty(HttpConstants.HEADER_TRANSFER_ENCODING));
        assertNotNull(reply.getOutboundProperty(HttpConstants.HEADER_CONTENT_LENGTH));
    }
    
}
