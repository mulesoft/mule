/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.messaging.meps;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.api.MuleMessage;
import org.mule.api.transformer.TransformerException;
import org.mule.api.transport.NoReceiverForEndpointException;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.exceptions.FunctionalTestException;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

/**
 * see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/messaging/meps/synchronous-response-exception-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/messaging/meps/synchronous-response-exception-flow.xml"}});
    }

    public SynchronousResponseExceptionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
