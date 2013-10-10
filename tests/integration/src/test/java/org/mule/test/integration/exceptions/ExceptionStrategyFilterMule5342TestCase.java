/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.api.routing.filter.Filter;
import org.mule.api.routing.filter.FilterUnacceptedException;
import org.mule.api.transport.DispatchException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class ExceptionStrategyFilterMule5342TestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-filter-mule-5342.xml";
    }

    @Test
    public void exceptionThrownFromMessageFilterIsHandledByExceptionHandler() throws Exception
    {
        try
        {
            MuleClient client = muleContext.getClient();
            client.send("vm://in", TEST_MESSAGE, null);
        }
        catch (DispatchException e)
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
