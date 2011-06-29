/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.exceptions;

import org.mule.api.MessagingException;
import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.tck.FunctionalTestCase;

import org.junit.Test;

public class ExceptionStrategyFilterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-filter.xml";
    }

    @Test
    public void testExceptionThrownFromMessageFilterIsHandledByExceptionHandler() throws Exception
    {
        try {
            muleContext.getClient().send("vm://in", TEST_MESSAGE, null);
            fail("Message Filter should have thrown FilterUnacceptedException");
        } catch(MessagingException e) {
            // Exception expected
        }
    }

    public static class FalseFilter implements Filter
    {
        @Override
        public boolean accept(MuleMessage message)
        {
            return false;
        }
    }
}
