/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.processor.MessageProcessor;
import org.mule.runtime.core.exception.TemplateMessagingExceptionStrategy;

import org.junit.Test;

public class ExceptionStrategyLifecycleTestCase extends AbstractIntegrationTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/exceptions/default-exception-strategy-lifecycle.xml";
    }

    @Test
    public void testLifecycle() throws Exception
    {
        FlowConstruct flowA = getFlowConstruct("flowA");
        FlowConstruct flowB = getFlowConstruct("flowB");
        TemplateMessagingExceptionStrategy flowAExceptionStrategy = (TemplateMessagingExceptionStrategy) flowA.getExceptionListener();
        TemplateMessagingExceptionStrategy flowBExceptionStrategy = (TemplateMessagingExceptionStrategy) flowB.getExceptionListener();
        LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowA = (LifecycleCheckerMessageProcessor) flowAExceptionStrategy.getMessageProcessors().get(0);
        LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowB = (LifecycleCheckerMessageProcessor) flowBExceptionStrategy.getMessageProcessors().get(0);
        assertThat(lifecycleCheckerMessageProcessorFlowA.isInitialized(), is(true));
        assertThat(lifecycleCheckerMessageProcessorFlowB.isInitialized(), is(true));
        assertThat(flowAExceptionStrategy.isInitialised(), is(true));
        assertThat(flowBExceptionStrategy.isInitialised(), is(true));
        ((Lifecycle)flowA).stop();
        assertThat(lifecycleCheckerMessageProcessorFlowA.isStopped(),is(true));
        assertThat(lifecycleCheckerMessageProcessorFlowB.isStopped(),is(false));

        FlowConstruct flowC = getFlowConstruct("flowC");
        FlowConstruct flowD = getFlowConstruct("flowD");
        TemplateMessagingExceptionStrategy flowCExceptionStrategy = (TemplateMessagingExceptionStrategy) flowC.getExceptionListener();
        TemplateMessagingExceptionStrategy flowDExceptionStrategy = (TemplateMessagingExceptionStrategy) flowD.getExceptionListener();
        LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowC = (LifecycleCheckerMessageProcessor) flowCExceptionStrategy.getMessageProcessors().get(0);
        LifecycleCheckerMessageProcessor lifecycleCheckerMessageProcessorFlowD = (LifecycleCheckerMessageProcessor) flowDExceptionStrategy.getMessageProcessors().get(0);
        assertThat(lifecycleCheckerMessageProcessorFlowC.isInitialized(), is(true));
        assertThat(lifecycleCheckerMessageProcessorFlowD.isInitialized(), is(true));
        assertThat(flowCExceptionStrategy.isInitialised(), is(true));
        assertThat(flowDExceptionStrategy.isInitialised(), is(true));
        ((Lifecycle)flowC).stop();
        assertThat(lifecycleCheckerMessageProcessorFlowC.isStopped(),is(true));
        assertThat(lifecycleCheckerMessageProcessorFlowD.isStopped(),is(false));
    }

    public static class LifecycleCheckerMessageProcessor implements MessageProcessor, Lifecycle
    {
        private boolean initialized;
        private boolean disposed;
        private boolean started;
        private boolean stopped;

        @Override
        public MuleEvent process(MuleEvent event) throws MuleException
        {
            return event;
        }

        @Override
        public void dispose()
        {
            disposed = true;
        }

        @Override
        public void initialise() throws InitialisationException
        {
            initialized = true;
        }

        @Override
        public void start() throws MuleException
        {
            started = true;
        }

        @Override
        public void stop() throws MuleException
        {
            stopped = true;
        }

        public boolean isInitialized()
        {
            return initialized;
        }

        public boolean isDisposed()
        {
            return disposed;
        }

        public boolean isStarted()
        {
            return started;
        }

        public boolean isStopped()
        {
            return stopped;
        }
    }
}
