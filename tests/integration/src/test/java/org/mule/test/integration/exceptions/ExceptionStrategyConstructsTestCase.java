/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.MuleMessage;
import org.mule.api.processor.MessageProcessor;
import org.mule.message.ExceptionMessage;
import org.mule.module.client.MuleClient;
import org.mule.tck.exceptions.FunctionalTestException;
import org.mule.tck.junit4.FunctionalTestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ExceptionStrategyConstructsTestCase extends FunctionalTestCase
{
    protected transient Log logger = LogFactory.getLog(getClass());

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-constructs-config.xml";
    }

    @Test
    public void testDefaultExceptionStrategySingleEndpoint() throws Exception
    {
        MuleClient mc = new MuleClient(muleContext);
       
        mc.dispatch("vm://inservice2", "test", null);
        assertExceptionMessage(mc.request("vm://modelout", RECEIVE_TIMEOUT));

        mc.dispatch("vm://inservice1", "test", null);
        assertExceptionMessage(mc.request("vm://service1out", RECEIVE_TIMEOUT));

        // request one more time to ensure the model's exception strategy did not run
        assertNull(mc.request("vm://modelout", RECEIVE_TIMEOUT));

        mc.dispatch("vm://inflow1", "test", null);
        assertExceptionMessage(mc.request("vm://flow1out", RECEIVE_TIMEOUT));

        // request one more time to ensure the model's exception strategy did not run
        assertNull(mc.request("vm://modelout", RECEIVE_TIMEOUT));

        mc.send("vm://inss1", "test", null);
        assertExceptionMessage(mc.request("vm://ss1out", RECEIVE_TIMEOUT));

        // request one more time to ensure the model's exception strategy did not run
        assertNull(mc.request("vm://modelout", RECEIVE_TIMEOUT));

        mc.send("vm://inss2", "test", null);
        MuleMessage modelError = mc.request("vm://modelout", RECEIVE_TIMEOUT);

        // This should not be null.  MULE-5087
        assertEquals(null, modelError);
    }

    private void assertExceptionMessage(MuleMessage out)
    {
        assertTrue(out.getPayload() instanceof ExceptionMessage);
        ExceptionMessage exceptionMessage = (ExceptionMessage) out.getPayload();
        assertTrue(exceptionMessage.getException().getClass() == FunctionalTestException.class ||
                   exceptionMessage.getException().getCause().getClass() == FunctionalTestException.class);
        assertEquals("test", exceptionMessage.getPayload());
    }

    public static class ExceptionThrowingProcessor implements MessageProcessor
    {
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            throw new FunctionalTestException();
        }
    }

    public static class ExceptionThrowingComponent
    {
        public byte[] process(byte[] msg) throws MuleException
        {
            throw new FunctionalTestException();
        }
    }
    
}
