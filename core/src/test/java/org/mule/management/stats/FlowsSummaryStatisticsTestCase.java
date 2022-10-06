/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management.stats;

import static org.mule.construct.AbstractFlowConstruct.INITIAL_STATE_STOPPED;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.source.MessageSource;
import org.mule.construct.AbstractPipeline;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.size.SmallTest;

import org.junit.Test;

/**
 * Note: flow-mappings are not considered
 *
 */
@SmallTest
public class FlowsSummaryStatisticsTestCase extends AbstractMuleContextTestCase
{

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
    
        muleContext = spy(muleContext);
    }
  
    @Test
    public void triggerFlow() throws MuleException
    {
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow = new TestPipeline("", muleContext);
        flow.setMessageSource(mock(MessageSource.class));

        flow.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    @Test
    public void apikitFlow() throws MuleException
    {
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow = new TestPipeline("get:/reservation:api-config", muleContext);
    
        flow.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 1, 0);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 1, 0, 0, 0, 0);
    
        flow.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    @Test
    public void apikitWithSourceFlow() throws MuleException
    {
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow = new TestPipeline("get:/reservation:api-config", muleContext);
        flow.setMessageSource(mock(MessageSource.class));

        flow.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    @Test
    public void privateFlow() throws MuleException
    {
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow = new TestPipeline("", muleContext);
    
        flow.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 1);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 1, 0, 0, 0);
    
        flow.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    @Test
    public void twoTriggerFlow() throws MuleException
    {
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow1 = new TestPipeline("", muleContext);
        flow1.setMessageSource(mock(MessageSource.class));
        TestPipeline flow2 = new TestPipeline("", muleContext);
        flow2.setMessageSource(mock(MessageSource.class));

        flow1.initialise();
        flow2.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 0, 0, 0);
    
        flow1.start();
        flow2.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 2, 0, 0);
    
        flow1.stop();
        flow2.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 2, 0, 0, 0, 0, 0);
    
        flow1.dispose();
        flow2.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    @Test
    public void triggerFlowRestarted() throws MuleException
    {
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow = new TestPipeline("", muleContext);
        flow.setMessageSource(mock(MessageSource.class));

        flow.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    @Test
    public void triggerFlowInitialStateStopped() throws MuleException
    {
        doReturn(true).when(muleContext).isStarting();
    
        AllStatistics statistics = muleContext.getStatistics();
        FlowsSummaryStatistics flowsSummaryStatistics = statistics.getFlowSummaryStatistics();

        TestPipeline flow = new TestPipeline("", muleContext);
        flow.setMessageSource(mock(MessageSource.class));
        flow.setInitialState(INITIAL_STATE_STOPPED);
    
        flow.initialise();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        doReturn(false).when(muleContext).isStarting();
        flow.start();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 1, 0, 0);
    
        flow.stop();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 1, 0, 0, 0, 0, 0);
    
        flow.dispose();
        assertFlowsSummaryStatistics(flowsSummaryStatistics, 0, 0, 0, 0, 0, 0);
    }
  
    private void assertFlowsSummaryStatistics(FlowsSummaryStatistics flowsSummaryStatistics,
                                              int expectedDeclaredTriggerFlows,
                                              int expectedDeclaredApikitFlows,
                                              int expectedDeclaredPrivateFlows,
                                              int expectedActiveTriggerFlows,
                                              int expectedActiveApikitFlows,
                                              int expectedActivePrivateFlows)
    {
        assertThat("declaredTriggerFlows",
                   flowsSummaryStatistics.getDeclaredTriggerFlows(), is(expectedDeclaredTriggerFlows));
        assertThat("declaredApikitFlows",
                   flowsSummaryStatistics.getDeclaredApikitFlows(), is(expectedDeclaredApikitFlows));
        assertThat("declaredPrivateFlows",
                   flowsSummaryStatistics.getDeclaredPrivateFlows(), is(expectedDeclaredPrivateFlows));
        assertThat("activeTriggerFlows",
                   flowsSummaryStatistics.getActiveTriggerFlows(), is(expectedActiveTriggerFlows));
        assertThat("activeApikitFlows",
                   flowsSummaryStatistics.getActiveApikitFlows(), is(expectedActiveApikitFlows));
        assertThat("activePrivateFlows",
                   flowsSummaryStatistics.getActivePrivateFlows(), is(expectedActivePrivateFlows));
    }
  
    private static class TestPipeline extends AbstractPipeline
    {
  
        public TestPipeline(String name, MuleContext muleContext)
        {
            super(name, muleContext);
        }
        
        @Override
        public String getConstructType()
        {
            return "Flow";
        }
    }
  
}
