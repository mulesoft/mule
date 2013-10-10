/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.management;

import org.mule.api.MuleMessage;
import org.mule.management.stats.ApplicationStatistics;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.ServiceStatistics;
import org.mule.module.client.MuleClient;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Iterator;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class JmxStatisticsAsyncTestCase extends FunctionalTestCase
{
    
    @Override
    protected String getConfigResources()
    {
        return "jmx-statistics-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();
                
        MuleClient muleClient = new MuleClient(muleContext);
        muleClient.dispatch("vm://in", "Hello world", null);
        MuleMessage response = muleClient.request("vm://out", RECEIVE_TIMEOUT * 2);
        assertNotNull(response);
        assertEquals("data", response.getPayloadAsString());
        muleClient.dispatch("vm://inflow", "Flow data", null);
        response = muleClient.request("vm://outflow", RECEIVE_TIMEOUT * 2);
        muleClient.dispatch("vm://inflow", "Flow data", null);
        response = muleClient.request("vm://outflow", RECEIVE_TIMEOUT * 2);
        assertNotNull(response);
        assertEquals("Flow data", response.getPayloadAsString());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        getServiceStatistics().clear();
        super.doTearDown();
    }

    @Test
    public void testCorrectAverageQueueSize() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAverageQueueSize());
    }

    @Test
    public void testCorrectAsynchEventsReceived() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAsyncEventsReceived());
        FlowConstructStatistics fstats = getFlowConstructStatistics();
        assertEquals(2, fstats.getAsyncEventsReceived());
        ApplicationStatistics astats = getApplicationStatistics();
        assertEquals(3, astats.getAsyncEventsReceived());
    }

    @Test
    public void testCorrectMaxQueueSize() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getMaxQueueSize());
    }

    @Test
    public void testCorrectAsynchEventsSent() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAsyncEventsSent());
    }

    @Test
    public void testCorrectTotalEventsSent() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getTotalEventsSent());
    }

    @Test
    public void testCorrectTotalEventsReceived() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getTotalEventsReceived());
        FlowConstructStatistics fstats = getFlowConstructStatistics();
        assertEquals(2, fstats.getTotalEventsReceived());
        ApplicationStatistics astats = getApplicationStatistics();
        assertEquals(3, astats.getTotalEventsReceived());
    }

    private ServiceStatistics getServiceStatistics()
    {
        Iterator<FlowConstructStatistics> iterator = muleContext.getStatistics().getServiceStatistics().iterator();
        FlowConstructStatistics stat1;
        do
        {
            assertTrue(iterator.hasNext());
            stat1 = iterator.next();
        }
        while (!(stat1 instanceof ServiceStatistics));
        return (ServiceStatistics)stat1;

    }

    private FlowConstructStatistics getFlowConstructStatistics()
    {
        Iterator<FlowConstructStatistics> iterator = muleContext.getStatistics().getServiceStatistics().iterator();
        FlowConstructStatistics stat1;
        do
        {
            assertTrue(iterator.hasNext());
            stat1 = iterator.next();
        }
        while (stat1.getClass() != FlowConstructStatistics.class);
        return stat1;
    }

    private ApplicationStatistics getApplicationStatistics()
    {
        Iterator<FlowConstructStatistics> iterator = muleContext.getStatistics().getServiceStatistics().iterator();
        FlowConstructStatistics stat1;
        do
        {
            assertTrue(iterator.hasNext());
            stat1 = iterator.next();
        }
        while (!(stat1 instanceof ApplicationStatistics));
        return (ApplicationStatistics)stat1;
    }

}
