/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.routing.filter.Filter;
import org.mule.message.ExceptionMessage;
import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ExceptionStrategyFilterMule5342TestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-filter-mule-5342.xml";
    }

    @Test
    public void testExceptionThrownFromMessageFilterIsHandledByExceptionHandler() throws Exception
    {
        MuleClient client = muleContext.getClient();
        client.send("vm://in", TEST_MESSAGE, null);
        MuleMessage handleException = client.request("vm://handleException", RECEIVE_TIMEOUT);
        assertNotNull(handleException);
        assertTrue(handleException.getPayload() instanceof ExceptionMessage);
        assertEquals(TEST_MESSAGE, ((ExceptionMessage) handleException.getPayload()).getPayload());
    }
    
    public static class FalseFilter implements Filter
    {
        public boolean accept(MuleMessage message)
        {
            return false;
        }
    }
}
