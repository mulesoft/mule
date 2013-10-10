/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.ExceptionPayload;
import org.mule.api.MuleMessage;
import org.mule.api.endpoint.MalformedEndpointException;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.module.client.MuleClient;
import org.mule.module.client.RemoteDispatcher;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.exceptions.FunctionalTestException;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class RemoteExceptionTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/client/remote-exception-config-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/client/remote-exception-config-flow.xml"}
        });
    }

    public RemoteExceptionTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
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
