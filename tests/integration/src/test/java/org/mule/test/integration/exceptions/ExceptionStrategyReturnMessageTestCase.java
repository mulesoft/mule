/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.MessagingException;
import org.mule.api.MuleEvent;
import org.mule.api.MuleMessage;
import org.mule.component.ComponentException;
import org.mule.exception.AbstractMessagingExceptionStrategy;
import org.mule.functional.junit4.FunctionalTestCase;
import org.mule.api.message.NullPayload;

import org.junit.Test;

public class ExceptionStrategyReturnMessageTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-return-message.xml";
    }

    @Test
    public void testReturnPayloadDefaultStrategy() throws Exception
    {
        try
        {
            flowRunner("InputService2").withPayload(getTestMuleMessage("Test Message")).run();
        }
        catch(ComponentException e)
        {
            assertNotNull(e.getEvent().getMessage().getPayload());
            assertTrue(e.getEvent().getMessage().getPayload() instanceof NullPayload);
        }
    }

    @Test
    public void testReturnPayloadCustomStrategy() throws Exception
    {
        MuleMessage msg = flowRunner("InputService").withPayload(getTestMuleMessage("Test Message")).run().getMessage();

        assertNotNull(msg);
        assertNotNull(msg.getExceptionPayload());
        assertEquals("Functional Test Service Exception", msg.getExceptionPayload().getMessage());

        assertNotNull(msg.getPayload());
        assertEquals("Ka-boom!", msg.getPayload());
    }

    public static class TestExceptionStrategy extends AbstractMessagingExceptionStrategy
    {
        @Override
        public MuleEvent handleException(Exception exception, MuleEvent event)
        {
            MuleEvent result = super.handleException(exception, event);
            result.getMessage().setPayload("Ka-boom!");
            if (exception instanceof MessagingException)
            {
                ((MessagingException)exception).setHandled(true);
            }

            return result;
        }
    }

}
