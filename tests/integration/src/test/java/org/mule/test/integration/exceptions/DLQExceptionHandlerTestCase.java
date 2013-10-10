/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.AbstractServiceAndFlowTestCase;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DLQExceptionHandlerTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE, "org/mule/test/integration/exceptions/exception-dlq-service.xml"},
            {ConfigVariant.FLOW, "org/mule/test/integration/exceptions/exception-dlq-flow.xml"}});
    }

    public DLQExceptionHandlerTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDLQ() throws Exception
    {
        MuleClient client = new MuleClient(muleContext);
        client.dispatch("jms://request.queue", "testing 1 2 3", null);

        MuleMessage message = client.request("jms://out.queue", 3000);
        assertNull(message);

        try
        {
            message = client.request("jms://DLQ", 20000);
        }
        catch (MuleException e)
        {
            e.printStackTrace(System.err);
        }
        assertNotNull(message);

        ExceptionMessage em = (ExceptionMessage) message.getPayload();
        assertEquals("testing 1 2 3", em.getPayload());
    }
}
