/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.routing.filter.Filter;
import org.mule.runtime.core.api.routing.filter.FilterUnacceptedException;

import org.junit.Test;

public class ExceptionStrategyFilterMule5342TestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-filter-mule-5342.xml";
    }

    @Test(expected = FilterUnacceptedException.class)
    public void exceptionThrownFromMessageFilterIsHandledByExceptionHandler() throws Exception
    {
        flowRunner("filter").withPayload(TEST_MESSAGE).run();
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
