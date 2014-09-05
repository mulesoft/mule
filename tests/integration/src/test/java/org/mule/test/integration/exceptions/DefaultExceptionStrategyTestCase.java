/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.exception.CatchMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

public class DefaultExceptionStrategyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/default-exception-strategy.xml";
    }

    public static class CustomExceptionStrategy implements MessagingExceptionHandler
    {

        @Override
        public MuleEvent handleException(Exception exception, MuleEvent event)
        {
            return null;
        }
    }

    @Test
    public void testFlowAndServiceUseProperExceptionStrategy()
    {
        Set<MessagingExceptionHandler> usedExceptionStrategies = new HashSet<MessagingExceptionHandler>();

        FlowConstruct flowNoExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("flowNoExceptionStrategy");
        MessagingExceptionHandler flowNoExceptionStrategyExceptionListener = flowNoExceptionStrategy.getExceptionListener();
        assertThat(flowNoExceptionStrategyExceptionListener, instanceOf(CustomExceptionStrategy.class));
        assertThat(usedExceptionStrategies.add(flowNoExceptionStrategyExceptionListener), is(true));

        FlowConstruct flowExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("flowExceptionStrategy");
        MessagingExceptionHandler flowExceptionStrategyExceptionListener = flowExceptionStrategy.getExceptionListener();
        assertThat(flowExceptionStrategyExceptionListener, instanceOf(CatchMessagingExceptionStrategy.class));
        assertThat(usedExceptionStrategies.add(flowExceptionStrategyExceptionListener), is(true));

    }

}
