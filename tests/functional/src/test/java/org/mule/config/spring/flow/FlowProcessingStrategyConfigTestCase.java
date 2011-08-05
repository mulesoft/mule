/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.spring.flow;

import org.mule.api.MuleContext;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.api.processor.ProcessingStrategy;
import org.mule.construct.Flow;
import org.mule.construct.flow.DefaultFlowProcessingStrategy;
import org.mule.processor.AsyncDelegateMessageProcessor;
import org.mule.processor.strategy.AsynchronousProcessingStrategy;
import org.mule.processor.strategy.QueuedAsynchronousProcessingStrategy;
import org.mule.processor.strategy.SynchronousProcessingStrategy;
import org.mule.tck.FunctionalTestCase;

import java.util.List;

public class FlowProcessingStrategyConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/spring/flow/flow-processing-strategies.xml";
    }

    public void testDefault() throws Exception
    {
        assertEquals(DefaultFlowProcessingStrategy.class,
            getFlowProcesingStrategy("defaultFlow").getClass());
    }

    public void testSynchronous() throws Exception
    {
        assertEquals(SynchronousProcessingStrategy.class,
            getFlowProcesingStrategy("synchronousFlow").getClass());
    }

    public void testAsynchronous() throws Exception
    {
        assertEquals(AsynchronousProcessingStrategy.class,
            getFlowProcesingStrategy("asynchronousFlow").getClass());
    }

    public void testQueuedAsynchronous() throws Exception
    {
        assertEquals(QueuedAsynchronousProcessingStrategy.class,
            getFlowProcesingStrategy("queuedAsynchronousFlow").getClass());
    }

    public void testCustomAsynchronous() throws Exception
    {
        ProcessingStrategy processingStrategy = getFlowProcesingStrategy("customAsynchronousFlow");

        assertEquals(AsynchronousProcessingStrategy.class, processingStrategy.getClass());

        assertAsynchronousStrategyConfig((AsynchronousProcessingStrategy) processingStrategy);
    }

    public void testCustomQueuedAsynchronous() throws Exception
    {
        ProcessingStrategy processingStrategy = getFlowProcesingStrategy("customQueuedAsynchronousFlow");

        assertEquals(QueuedAsynchronousProcessingStrategy.class, processingStrategy.getClass());

        assertAsynchronousStrategyConfig((AsynchronousProcessingStrategy) processingStrategy);

        assertEquals(100, ((QueuedAsynchronousProcessingStrategy) processingStrategy).getQueueTimeout()
            .intValue());
        assertEquals(10, ((QueuedAsynchronousProcessingStrategy) processingStrategy).getMaxQueueSize()
            .intValue());

    }

    public void testCustom() throws Exception
    {
        ProcessingStrategy processingStrategy = getFlowProcesingStrategy("customProcessingStrategyFlow");
        assertEquals(CustomProcessingStrategy.class, processingStrategy.getClass());

        assertEquals("bar", (((CustomProcessingStrategy) processingStrategy).foo));
    }

    public void testDefaultAsync() throws Exception
    {
        assertEquals(QueuedAsynchronousProcessingStrategy.class,
            getAsyncProcesingStrategy("defaultAsync").getClass());
    }

    public void testAsynchronousAsync() throws Exception
    {
        assertEquals(AsynchronousProcessingStrategy.class,
            getAsyncProcesingStrategy("asynchronousAsync").getClass());
    }

    public void testQueuedAsynchronousAsync() throws Exception
    {
        assertEquals(QueuedAsynchronousProcessingStrategy.class,
            getAsyncProcesingStrategy("queuedAsynchronousAsync").getClass());
    }

    public void testCustomAsynchronousAsync() throws Exception
    {
        ProcessingStrategy processingStrategy = getAsyncProcesingStrategy("customAsynchronousAsync");

        assertEquals(AsynchronousProcessingStrategy.class, processingStrategy.getClass());

        assertAsynchronousStrategyConfig((AsynchronousProcessingStrategy) processingStrategy);
    }

    public void testCustomQueuedAsynchronousAsync() throws Exception
    {
        ProcessingStrategy processingStrategy = getAsyncProcesingStrategy("customQueuedAsynchronousAsync");

        assertEquals(QueuedAsynchronousProcessingStrategy.class, processingStrategy.getClass());

        assertAsynchronousStrategyConfig((AsynchronousProcessingStrategy) processingStrategy);

        assertEquals(100, ((QueuedAsynchronousProcessingStrategy) processingStrategy).getQueueTimeout()
            .intValue());
        assertEquals(10, ((QueuedAsynchronousProcessingStrategy) processingStrategy).getMaxQueueSize()
            .intValue());

    }

    public void testCustomAsync() throws Exception
    {
        ProcessingStrategy processingStrategy = getAsyncProcesingStrategy("customProcessingStrategyAsync");
        assertEquals(CustomProcessingStrategy.class, processingStrategy.getClass());

        assertEquals("bar", (((CustomProcessingStrategy) processingStrategy).foo));
    }

    private void assertAsynchronousStrategyConfig(AsynchronousProcessingStrategy processingStrategy)
    {
        assertEquals(10, processingStrategy.getMaxThreads().intValue());
        assertEquals(5, processingStrategy.getMinThreads().intValue());
        assertEquals(100, processingStrategy.getThreadTTL().intValue());
        assertEquals(10, processingStrategy.getMaxBufferSize().intValue());

    }

    private ProcessingStrategy getFlowProcesingStrategy(String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        return flow.getProcessingStrategy();
    }

    private ProcessingStrategy getAsyncProcesingStrategy(String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        MessageProcessor processor = flow.getMessageProcessors().get(0);
        assertEquals(AsyncDelegateMessageProcessor.class, processor.getClass());
        return ((AsyncDelegateMessageProcessor) processor).getProcessingStrategy();
    }

    public static class CustomProcessingStrategy implements ProcessingStrategy
    {

        String foo;

        @Override
        public void configureProcessors(List<MessageProcessor> processors,
                                        ThreadNameSource nameSource,
                                        MessageProcessorChainBuilder chainBuilder,
                                        MuleContext muleContext)
        {
            // TODO Auto-generated method stub
        }

        public void setFoo(String foo)
        {
            this.foo = foo;

        }
    }

}
