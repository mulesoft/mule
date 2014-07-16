/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.processor.MessageProcessor;
import org.mule.construct.Flow;
import org.mule.endpoint.DefaultInboundEndpoint;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.jobs.CustomJobConfig;
import org.mule.transport.quartz.jobs.CustomJobFromMessageConfig;
import org.mule.transport.quartz.jobs.EndpointPollingJob;
import org.mule.transport.quartz.jobs.EndpointPollingJobConfig;
import org.mule.transport.quartz.jobs.EventGeneratorJob;
import org.mule.transport.quartz.jobs.EventGeneratorJobConfig;
import org.mule.transport.quartz.jobs.ScheduledDispatchJob;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

import java.util.List;

import org.junit.Test;
import org.quartz.impl.StdScheduler;

/**
 * Tests the "quartz" namespace.
 */
public class QuartzNamespaceHandlerFlowTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "quartz-namespace-config-flow.xml";
    }

    @Test
    public void testDefaultConfig() throws Exception
    {
        QuartzConnector c = (QuartzConnector) muleContext.getRegistry().lookupConnector(
            "quartzConnectorDefaults");
        assertNotNull(c);

        assertNotNull(c.getQuartzScheduler());
        assertEquals(StdScheduler.class, c.getQuartzScheduler().getClass());
        StdScheduler scheduler = (StdScheduler) c.getQuartzScheduler();
        String defaultSchedulerName = "scheduler-" + muleContext.getConfiguration().getId();
        assertEquals(defaultSchedulerName, scheduler.getSchedulerName());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testInjectedSchedulerBean() throws Exception
    {
        QuartzConnector c = (QuartzConnector) muleContext.getRegistry().lookupConnector("quartzConnector1");
        assertNotNull(c);

        assertNotNull(c.getQuartzScheduler());
        assertEquals(StdScheduler.class, c.getQuartzScheduler().getClass());
        StdScheduler scheduler = (StdScheduler) c.getQuartzScheduler();
        assertEquals("MuleScheduler1", scheduler.getSchedulerName());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testFactoryProperties() throws Exception
    {
        QuartzConnector c = (QuartzConnector) muleContext.getRegistry().lookupConnector("quartzConnector2");
        assertNotNull(c);

        assertNotNull(c.getQuartzScheduler());
        assertEquals(StdScheduler.class, c.getQuartzScheduler().getClass());
        StdScheduler scheduler = (StdScheduler) c.getQuartzScheduler();
        assertEquals("MuleScheduler2", scheduler.getSchedulerName());

        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    @Test
    public void testEndpoint1Config() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("testService1");

        assertNotNull(flow);

        InboundEndpoint ep = (DefaultInboundEndpoint) flow.getMessageSource();
        assertNotNull(ep);
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof EventGeneratorJobConfig);
        EventGeneratorJobConfig config = (EventGeneratorJobConfig) ep.getProperty("jobConfig");
        assertEquals("foo", config.getPayload());
    }

    @Test
    public void testEndpoint2Config() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("testService2");

        assertNotNull(flow);

        InboundEndpoint ep = (InboundEndpoint) flow.getMessageSource();
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof EventGeneratorJobConfig);
        EventGeneratorJobConfig config = (EventGeneratorJobConfig) ep.getProperty("jobConfig");
        assertEquals("foo bar", config.getPayload());
    }

    @Test
    public void testEndpoint3Config() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("testService3");

        assertNotNull(flow);
        
        OutboundEndpoint ep = (OutboundEndpoint) getMessageProcessors(flow).get(1);
        
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof CustomJobFromMessageConfig);
        CustomJobFromMessageConfig config = (CustomJobFromMessageConfig) ep.getProperty("jobConfig");
        assertEquals("header", config.getEvaluator());
        assertEquals("jobConfig", config.getExpression());
        assertNull(config.getCustomEvaluator());
        // Test grabbing the Job instance
    }

    @Test
    public void testEndpoint4Config() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("testService4");

        assertNotNull(flow);

        OutboundEndpoint ep = (OutboundEndpoint) getMessageProcessors(flow).get(1);

        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof CustomJobConfig);
        CustomJobConfig config = (CustomJobConfig) ep.getProperty("jobConfig");
        assertTrue(config.getJob() instanceof EventGeneratorJob);
    }

    @Test
    public void testEndpoint5Config() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("testService5");

        assertNotNull(flow);
        
        InboundEndpoint ep = (InboundEndpoint) flow.getMessageSource();

        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof EndpointPollingJobConfig);
        EndpointPollingJobConfig config = (EndpointPollingJobConfig) ep.getProperty("jobConfig");
        assertEquals(EndpointPollingJob.class, config.getJobClass());
        assertEquals("file:///N/drop-data/in", config.getEndpointRef());
        assertEquals(4000, config.getTimeout());
    }

    @Test
    public void testEndpoint6Config() throws Exception
    {
        Flow flow = (Flow) muleContext.getRegistry().lookupFlowConstruct("testService6");

        assertNotNull(flow);
        
        OutboundEndpoint ep = (OutboundEndpoint) getMessageProcessors(flow).get(1);

        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof ScheduledDispatchJobConfig);
        ScheduledDispatchJobConfig config = (ScheduledDispatchJobConfig) ep.getProperty("jobConfig");
        assertEquals(ScheduledDispatchJob.class, config.getJobClass());
        assertEquals("scheduledDispatchEndpoint", config.getEndpointRef());
    }

    private List<MessageProcessor> getMessageProcessors(Flow flow)
    {
        return flow.getMessageProcessors();
    }

}
