/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

/**
 * see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/messaging/meps/synchronous-response-exception-flow.xml";
    }

    @Test
    public void testComponentException() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://in1", "request", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(FunctionalTestException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
    }

    @Test
    public void testOutboundRoutingException() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://in2", "request", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(NoReceiverForEndpointException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
    }

    @Test
    public void testInboundTransformerException() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://in3", "request", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(TransformerException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
    }

    @Test
    public void testOutboundTransformerException() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://in4", "request", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(TransformerException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());
    }

    @Test
    public void testResponseTransformerException() throws Exception
    {
        MuleMessage message = muleContext.getClient().send("vm://in5", "request", null);
        assertNotNull(message);
        assertNotNull(message.getExceptionPayload());
        assertEquals(TransformerException.class, message.getExceptionPayload()
            .getRootException()
            .getClass());

    }
}
