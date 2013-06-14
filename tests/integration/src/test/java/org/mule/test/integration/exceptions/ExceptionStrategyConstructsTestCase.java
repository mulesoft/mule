/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.message.ExceptionMessage;
import org.mule.tck.AbstractServiceAndFlowTestCase;
import org.mule.tck.exceptions.FunctionalTestException;

import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

public class ExceptionStrategyConstructsTestCase extends AbstractServiceAndFlowTestCase
{
    @Parameters
    public static Collection<Object[]> parameters()
    {
        return Arrays.asList(new Object[][]{
            {ConfigVariant.SERVICE,
                "org/mule/test/integration/exceptions/exception-strategy-constructs-config-service.xml"},
            {ConfigVariant.FLOW,
                "org/mule/test/integration/exceptions/exception-strategy-constructs-config-flow.xml"}});
    }

    public ExceptionStrategyConstructsTestCase(ConfigVariant variant, String configResources)
    {
        super(variant, configResources);
    }

    @Test
    public void testDefaultExceptionStrategySingleEndpoint() throws Exception
    {
        MuleClient client = muleContext.getClient();

        client.dispatch("vm://inservice2", "test", null);
        assertExceptionMessage(client.request("vm://modelout", RECEIVE_TIMEOUT));

        client.dispatch("vm://inservice1", "test", null);
        assertExceptionMessage(client.request("vm://service1out", RECEIVE_TIMEOUT));

        // request one more time to ensure the model's exception strategy did not run
        assertNull(client.request("vm://modelout", RECEIVE_TIMEOUT));

        client.dispatch("vm://inflow1", "test", null);
        assertExceptionMessage(client.request("vm://flow1out", RECEIVE_TIMEOUT));

        // request one more time to ensure the model's exception strategy did not run
        assertNull(client.request("vm://modelout", RECEIVE_TIMEOUT));

        // The following tests no longer apply because if the exchange is synchronous
        // (which is hard-coded for <pattern:simple-service>), then the exception
        // will be
        // thrown back to the caller and no exception strategy will be invoked.
        /*
         * mc.send("vm://inss1", "test", null);
         * assertExceptionMessage(mc.request("vm://ss1out", RECEIVE_TIMEOUT)); //
         * request one more time to ensure the model's exception strategy did not run
         * assertNull(mc.request("vm://modelout", RECEIVE_TIMEOUT));
         * mc.send("vm://inss2", "test", null); MuleMessage modelError =
         * mc.request("vm://modelout", RECEIVE_TIMEOUT); // This should not be null.
         * MULE-5087 assertEquals(null, modelError);
         */
    }

    private void assertExceptionMessage(MuleMessage out)
    {
        assertNotNull(out);
        assertTrue(out.getPayload() instanceof ExceptionMessage);
        ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload();
        assertTrue(exceptionMessage.getException().getClass() == FunctionalTestException.class
                   || exceptionMessage.getException().getCause().getClass() == FunctionalTestException.class);
        assertEquals("test", exceptionMessage.getPayload());
    }

    public static class ExceptionThrowingProcessor implements MessageProcessor
    {
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new MessagingException(event,new FunctionalTestException());
        }
    }
}
