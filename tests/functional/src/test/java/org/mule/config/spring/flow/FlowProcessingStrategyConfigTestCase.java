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

import org.mule.api.construct.Pipeline;
import org.mule.api.construct.PipelineProcessingStrategy;
import org.mule.api.processor.MessageProcessorChainBuilder;
import org.mule.construct.AsynchronousProcessingStrategy;
import org.mule.construct.Flow;
import org.mule.construct.QueuedAsynchronousProcessingStrategy;
import org.mule.construct.SynchronousProcessingStrategy;
import org.mule.tck.FunctionalTestCase;

public class FlowProcessingStrategyConfigTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/config/spring/flow/flow-processing-strategies.xml";
    }

    public void testDefault() throws Exception
    {
        assertEquals(QueuedAsynchronousProcessingStrategy.class,
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
        PipelineProcessingStrategy processingStrategy = getFlowProcesingStrategy("customAsynchronousFlow");

        assertEquals(AsynchronousProcessingStrategy.class, processingStrategy.getClass());

        assertAsynchronousStrategyConfig((AsynchronousProcessingStrategy) processingStrategy);
    }

    private void assertAsynchronousStrategyConfig(AsynchronousProcessingStrategy processingStrategy)
    {
        assertEquals(10, processingStrategy.getMaxThreads().intValue());
        assertEquals(5, processingStrategy.getMinThreads().intValue());
        assertEquals(100, processingStrategy.getThreadTTL().intValue());
        assertEquals(10, processingStrategy.getMaxBufferSize().intValue());

    }

    public void testCustomQueuedAsynchronous() throws Exception
    {
        PipelineProcessingStrategy processingStrategy = getFlowProcesingStrategy("customQueuedAsynchronousFlow");

        assertEquals(QueuedAsynchronousProcessingStrategy.class, processingStrategy.getClass());

        assertAsynchronousStrategyConfig((AsynchronousProcessingStrategy) processingStrategy);

        assertEquals(100, ((QueuedAsynchronousProcessingStrategy) processingStrategy).getQueueTimeout()
            .intValue());
        assertEquals(10, ((QueuedAsynchronousProcessingStrategy) processingStrategy).getMaxQueueSize()
            .intValue());

    }

    public void testCustom() throws Exception
    {
        PipelineProcessingStrategy processingStrategy = getFlowProcesingStrategy("customProcessingStrategyFlow");
        assertEquals(CustomProcessingStrategy.class, processingStrategy.getClass());

        assertEquals("bar", (((CustomProcessingStrategy) processingStrategy).foo));
    }

    private PipelineProcessingStrategy getFlowProcesingStrategy(String flowName) throws Exception
    {
        Flow flow = (Flow) getFlowConstruct(flowName);
        return flow.getProcessingStrategy();
    }

    public static class CustomProcessingStrategy implements PipelineProcessingStrategy
    {

        String foo;

        @Override
        public void configureProcessors(Pipeline pipeline, MessageProcessorChainBuilder chainBuilder)
        {
            // TODO Auto-generated method stub

        }

        public void setFoo(String foo)
        {
            this.foo = foo;

        }
    }

}
