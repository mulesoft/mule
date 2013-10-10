/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.exceptions;

import org.junit.Test;
import org.mule.api.MuleEvent;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.service.Service;
import org.mule.exception.CatchMessagingExceptionStrategy;
import org.mule.exception.DefaultMessagingExceptionStrategy;
import org.mule.tck.junit4.FunctionalTestCase;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import java.util.HashSet;
import java.util.Set;

public class DefaultExceptionStrategyTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
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

        Service serviceNoExceptionStrategy = muleContext.getRegistry().lookupService("serviceNoExceptionStrategy");
        MessagingExceptionHandler serviceNoExceptionStrategyExceptionListener = serviceNoExceptionStrategy.getExceptionListener();
        assertThat(serviceNoExceptionStrategyExceptionListener, instanceOf(CatchMessagingExceptionStrategy.class));
        usedExceptionStrategies.add(serviceNoExceptionStrategyExceptionListener);

        Service serviceExceptionStrategy = muleContext.getRegistry().lookupService("serviceExceptionStrategy");
        MessagingExceptionHandler serviceExceptionStrategyExceptionListener = serviceExceptionStrategy.getExceptionListener();
        assertThat(serviceExceptionStrategyExceptionListener, instanceOf(DefaultMessagingExceptionStrategy.class));
        assertThat(usedExceptionStrategies.add(serviceExceptionStrategyExceptionListener), is(true));

        Service serviceNoExceptionStrategyInModel = muleContext.getRegistry().lookupService("serviceNoExceptionStrategyInModel");
        MessagingExceptionHandler serviceNoExceptionStrategyInModelExceptionListener = serviceNoExceptionStrategyInModel.getExceptionListener();
        assertThat(serviceNoExceptionStrategyInModelExceptionListener, instanceOf(CustomExceptionStrategy.class));
        assertThat(usedExceptionStrategies.add(serviceNoExceptionStrategyInModelExceptionListener),is(true));

        FlowConstruct flowNoExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("flowNoExceptionStrategy");
        MessagingExceptionHandler flowNoExceptionStrategyExceptionListener = flowNoExceptionStrategy.getExceptionListener();
        assertThat(flowNoExceptionStrategyExceptionListener, instanceOf(CustomExceptionStrategy.class));
        assertThat(usedExceptionStrategies.add(flowNoExceptionStrategyExceptionListener),is(true));

        FlowConstruct flowExceptionStrategy = muleContext.getRegistry().lookupFlowConstruct("flowExceptionStrategy");
        MessagingExceptionHandler flowExceptionStrategyExceptionListener = flowExceptionStrategy.getExceptionListener();
        assertThat(flowExceptionStrategyExceptionListener, instanceOf(CatchMessagingExceptionStrategy.class));
        assertThat(usedExceptionStrategies.add(flowExceptionStrategyExceptionListener),is(true));

    }

}
