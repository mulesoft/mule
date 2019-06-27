/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import org.mule.tck.junit4.FunctionalTestCase;

import org.junit.Test;

public class ExceptionStrategyWithScopeInsideTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/exception-strategy-with-scopes-config.xml";
    }

    @Test
    public void exceptionHandlerWithScatterGatherRouterIgnoresFlowException() throws Exception
    {
        runFlowWithPayloadAndExpect("error-handler-with-scatter-gather-router-should-ignore-previous-exception", "hello dog", "hello goat");
    }

    @Test(expected = Exception.class)
    public void exceptionHandlerWithScatterGatherRouterFailOnScopeException() throws Exception
    {
        runFlow("error-handler-with-scatter-gather-router-should-fail-on-scope-generate-exception", "hello walrus");
    }
}
