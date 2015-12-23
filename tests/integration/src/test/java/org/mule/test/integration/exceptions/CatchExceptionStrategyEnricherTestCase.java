/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.DefaultMuleMessage;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.client.LocalMuleClient;
import org.mule.api.processor.MessageProcessor;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class CatchExceptionStrategyEnricherTestCase extends FunctionalTestCase
{
    public static class ErrorProcessor implements MessageProcessor
    {
        private static Throwable handled;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            handled = event.getMessage().getExceptionPayload().getException();
            return event;
        }
    }

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/catch-exception-strategy-enricher.xml";
    }

    @Test
    public void testFlowRefHandlingException() throws Exception
    {
        LocalMuleClient client = muleContext.getClient();
        MuleMessage response = client.send("vm://inEnricherExceptionFlow", new DefaultMuleMessage("payload", muleContext));
        assertThat(ErrorProcessor.handled, not(nullValue()));
        assertThat(response.getExceptionPayload(), nullValue());
    }
}
