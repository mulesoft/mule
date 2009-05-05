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

import org.mule.api.ExceptionPayload;
import org.mule.api.DefaultMuleException;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.transformer.TransformerException;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.FunctionalTestCase;

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
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("tcp://localhost:25551");
        MuleMessage result = dispatcher.sendRemote("vm://test.queue.1", new Date(), null);
        assertNotNull(result);
        ExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getException().getCause() instanceof TransformerException);
        assertTrue(exceptionPayload.getRootException() instanceof Exception);
    }

    public void testClientMalformedEndpointException() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("tcp://localhost:25551");
        MuleMessage result = dispatcher.sendRemote("test.queue.2", new Date(), null);
        assertNotNull(result);
        ExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof MalformedEndpointException);
    }

    public void testClientComponentException() throws Exception
    {
        MuleClient client = new MuleClient();
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("tcp://localhost:25551");
        MuleMessage result = dispatcher.sendRemote("vm://test.queue.2", new Date(), null);
        assertNotNull(result);
        ExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof DefaultMuleException);
        assertEquals("Functional Test Service Exception", exceptionPayload.getRootException().getMessage());
    }

}
