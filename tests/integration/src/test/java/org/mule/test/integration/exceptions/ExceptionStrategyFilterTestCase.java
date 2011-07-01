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

import org.mule.api.MuleMessage;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.api.transport.DispatchException;
import org.mule.tck.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

public class ExceptionStrategyFilterTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-filter.xml";
    }

    public void testExceptionThrownFromMessageFilterIsSeenBySender() throws Exception
    {
        try
        {
            muleContext.getClient().send("vm://in", TEST_MESSAGE, null);
            fail("Message Filter should have thrown FilterUnacceptedException");
        }
        catch(DispatchException e)
        {
            assertThatRootCauseIsFilterUnacceptedException(e);
        }
    }

    private void assertThatRootCauseIsFilterUnacceptedException(DispatchException e)
    {
        int index = ExceptionUtils.indexOfThrowable(e, FilterUnacceptedException.class);
        assertTrue(index > -1);
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
