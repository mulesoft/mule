/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.message.ExceptionMessage;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ExceptionStrategyWithFlowExceptionTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-with-flow-exception.xml";
    }

    @Test
    public void testFlowExceptionExceptionStrategy() throws MuleException
    {
        MuleClient client = muleContext.getClient();
        client.dispatch("vm://in", TEST_MESSAGE, null);
        MuleMessage message = client.request("vm://out", RECEIVE_TIMEOUT);

        assertNotNull("request returned no message", message);
        assertTrue(message.getPayload() instanceof ExceptionMessage);
    }

    public static class ExceptionThrower implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new MessagingException(event,null);
        }
    }
}
