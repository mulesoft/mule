/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.client;

import org.mule.MuleException;
import org.mule.extras.client.MuleClient;
import org.mule.extras.client.RemoteDispatcher;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOExceptionPayload;
import org.mule.umo.UMOMessage;
import org.mule.umo.endpoint.MalformedEndpointException;
import org.mule.umo.transformer.TransformerException;

import java.util.Date;

public class RemoteExceptionTestCase extends FunctionalTestCase
{

    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/remote-exception-config.xml";
    }

    public void testClientTransformerException() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:5555");
        UMOMessage result = dispatcher.sendRemote("vm://test.queue.1", new Date(), null);
        assertNotNull(result);
        UMOExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof TransformerException);
    }

    public void testClientMalformedEndpointException() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:5555");
        UMOMessage result = dispatcher.sendRemote("test.queue.2", new Date(), null);
        assertNotNull(result);
        UMOExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof MalformedEndpointException);
    }

    public void testClientComponentException() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("http://localhost:5555");
        UMOMessage result = dispatcher.sendRemote("vm://test.queue.2", new Date(), null);
        assertNotNull(result);
        UMOExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof MuleException);
        assertEquals("Functional Test Component Exception", exceptionPayload.getRootException().getMessage());
    }

}
