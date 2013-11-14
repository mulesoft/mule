/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    protected String getConfigFile()
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
        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new MessagingException(event,null);
        }
    }
}
