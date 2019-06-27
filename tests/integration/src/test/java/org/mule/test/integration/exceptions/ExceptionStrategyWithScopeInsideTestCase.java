/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import org.mule.api.transformer.TransformerMessagingException;
import org.mule.routing.CompositeRoutingException;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.util.ExceptionUtils;

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
        runFlowWithPayloadAndExpect("scatter-gather-in-catch-es", "hello dog", "hello goat");
    }

    @Test()
    public void exceptionHandlerWithScatterGatherRouterFailOnScopeException() throws Exception
    {
        try
        {
            runFlow("scatter-gather-in-default-es", "hello walrus");
        }
        catch (Exception e)
        {
            assertThat(ExceptionUtils.getRootCause(e).getMessage(), is("KA-BOOM!"));
        }
    }
}
