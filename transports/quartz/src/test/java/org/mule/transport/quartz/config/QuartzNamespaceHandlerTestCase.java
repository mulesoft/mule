/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz.config;

import org.mule.api.endpoint.InboundEndpoint;
import org.mule.api.endpoint.OutboundEndpoint;
import org.mule.api.routing.OutboundRouter;
import org.mule.api.service.Service;
import org.mule.tck.FunctionalTestCase;
import org.mule.transport.quartz.QuartzConnector;
import org.mule.transport.quartz.jobs.CustomJobConfig;
import org.mule.transport.quartz.jobs.CustomJobFromMessageConfig;
import org.mule.transport.quartz.jobs.EndpointPollingJob;
import org.mule.transport.quartz.jobs.EndpointPollingJobConfig;
import org.mule.transport.quartz.jobs.EventGeneratorJob;
import org.mule.transport.quartz.jobs.EventGeneratorJobConfig;
import org.mule.transport.quartz.jobs.ScheduledDispatchJob;
import org.mule.transport.quartz.jobs.ScheduledDispatchJobConfig;

import org.quartz.impl.StdScheduler;


/**
 * Tests the "quartz" namespace.
 */
public class QuartzNamespaceHandlerTestCase extends FunctionalTestCase
{
    protected String getConfigResources()
    {
        return "quartz-namespace-config.xml";
    }

    public void testDefaultConfig() throws Exception
    {
        QuartzConnector c = (QuartzConnector)muleContext.getRegistry().lookupConnector("quartzConnectorDefaults");
        assertNotNull(c);
        
        assertNotNull(c.getQuartzScheduler());
        assertEquals(StdScheduler.class, c.getQuartzScheduler().getClass());
        StdScheduler scheduler = (StdScheduler) c.getQuartzScheduler();
        assertEquals("DefaultQuartzScheduler", scheduler.getSchedulerName());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testInjectedSchedulerBean() throws Exception
    {
        QuartzConnector c = (QuartzConnector)muleContext.getRegistry().lookupConnector("quartzConnector1");
        assertNotNull(c);
        
        assertNotNull(c.getQuartzScheduler());
        assertEquals(StdScheduler.class, c.getQuartzScheduler().getClass());
        StdScheduler scheduler = (StdScheduler) c.getQuartzScheduler();
        assertEquals("MuleScheduler1", scheduler.getSchedulerName());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }
    
    public void testFactoryProperties() throws Exception
    {
        QuartzConnector c = (QuartzConnector)muleContext.getRegistry().lookupConnector("quartzConnector2");
        assertNotNull(c);
        
        assertNotNull(c.getQuartzScheduler());
        assertEquals(StdScheduler.class, c.getQuartzScheduler().getClass());
        StdScheduler scheduler = (StdScheduler) c.getQuartzScheduler();
        assertEquals("MuleScheduler2", scheduler.getSchedulerName());
        
        assertTrue(c.isConnected());
        assertTrue(c.isStarted());
    }

    public void testEndpoint1Config() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService1");
        assertNotNull(service);

        InboundEndpoint ep = service.getInboundRouter().getEndpoint("qEP1");
        assertNotNull(ep);
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof EventGeneratorJobConfig);
        EventGeneratorJobConfig config = (EventGeneratorJobConfig)ep.getProperty("jobConfig");
        assertEquals("foo", config.getPayload());
    }

    public void testEndpoint2Config() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService2");
        assertNotNull(service);

        InboundEndpoint ep = service.getInboundRouter().getEndpoint("qEP2");
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof EventGeneratorJobConfig);
        EventGeneratorJobConfig config = (EventGeneratorJobConfig)ep.getProperty("jobConfig");
        assertEquals("foo bar", config.getPayload());
    }


    public void testEndpoint3Config() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService3");
        assertNotNull(service);

        OutboundEndpoint ep = ((OutboundRouter)service.getOutboundRouter().getRouters().get(0)).getEndpoint("qEP3");
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof CustomJobFromMessageConfig);
        CustomJobFromMessageConfig config = (CustomJobFromMessageConfig)ep.getProperty("jobConfig");
        assertEquals("header", config.getEvaluator());
        assertEquals("jobConfig", config.getExpression());
        assertNull(config.getCustomEvaluator());
        //Test grabbing the Job instance
    }



    public void testEndpoint4Config() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService4");
        assertNotNull(service);

        OutboundEndpoint ep = ((OutboundRouter)service.getOutboundRouter().getRouters().get(0)).getEndpoint("qEP4");

        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof CustomJobConfig);
        CustomJobConfig config = (CustomJobConfig)ep.getProperty("jobConfig");
        assertTrue(config.getJob() instanceof EventGeneratorJob);
    }

    public void testEndpoint5Config() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService5");
        assertNotNull(service);

        InboundEndpoint ep = service.getInboundRouter().getEndpoint("qEP5");
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof EndpointPollingJobConfig);
        EndpointPollingJobConfig config = (EndpointPollingJobConfig)ep.getProperty("jobConfig");
        assertEquals(EndpointPollingJob.class, config.getJobClass());
        assertEquals("file:///N/drop-data/in", config.getEndpointRef());
        assertEquals(4000, config.getTimeout());
    }

    public void testEndpoint6Config() throws Exception
    {
        Service service = muleContext.getRegistry().lookupService("testService6");
        assertNotNull(service);

        OutboundEndpoint ep = ((OutboundRouter)service.getOutboundRouter().getRouters().get(0)).getEndpoint("qEP6");
        
        assertNotNull(ep.getProperty("jobConfig"));
        assertTrue(ep.getProperty("jobConfig") instanceof ScheduledDispatchJobConfig);
        ScheduledDispatchJobConfig config = (ScheduledDispatchJobConfig)ep.getProperty("jobConfig");
        assertEquals(ScheduledDispatchJob.class, config.getJobClass());
        assertEquals("scheduledDispatchEndpoint", config.getEndpointRef());
    }
}
