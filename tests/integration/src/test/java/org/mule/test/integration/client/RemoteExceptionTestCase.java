/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.client;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Date;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class RemoteExceptionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/client/remote-exception-config.xml";
    }

    @Test
    public void testClientTransformerException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("tcp://localhost:25551");
        MuleMessage result = dispatcher.sendRemote("vm://test.queue.1", new Date(), null);
        assertNotNull(result);
        ExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getException() instanceof TransformerMessagingException);
        assertTrue(exceptionPayload.getRootException() instanceof Exception);
    }

    @Test
    public void testClientMalformedEndpointException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("tcp://localhost:25551");
        MuleMessage result = dispatcher.sendRemote("test.queue.2", new Date(), null);
        assertNotNull(result);
        ExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException() instanceof MalformedEndpointException);
    }

    @Test
    public void testClientComponentException() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        RemoteDispatcher dispatcher = client.getRemoteDispatcher("tcp://localhost:25551");
        MuleMessage result = dispatcher.sendRemote("vm://test.queue.2", new Date(), null);
        assertNotNull(result);
        ExceptionPayload exceptionPayload = result.getExceptionPayload();
        assertNotNull(exceptionPayload);
        assertTrue(exceptionPayload.getRootException().getClass().getName(), 
                   exceptionPayload.getRootException() instanceof FunctionalTestException);
        assertEquals("Functional Test Service Exception", exceptionPayload.getRootException().getMessage());
    }

}
