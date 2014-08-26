/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.management;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.api.MuleMessage;
import org.mule.api.client.MuleClient;
import org.mule.management.stats.ApplicationStatistics;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.tck.junit4.FunctionalTestCase;

import java.util.Iterator;

import org.junit.Test;

public class JmxStatisticsAsyncTestCase extends FunctionalTestCase
{
    @Override
    protected String getConfigFile()
    {
        return "jmx-statistics-test.xml";
    }

    @Override
    protected void doSetUp() throws Exception
    {
        super.doSetUp();

        MuleClient muleClient = muleContext.getClient();

        muleClient.dispatch("vm://in", "Flow data", null);
        MuleMessage response = muleClient.request("vm://out", RECEIVE_TIMEOUT * 2);
        assertNotNull(response);
        assertEquals("Flow data", response.getPayloadAsString());
        muleClient.dispatch("vm://in", "Flow data", null);
        response = muleClient.request("vm://out", RECEIVE_TIMEOUT * 2);
        assertNotNull(response);
        assertEquals("Flow data", response.getPayloadAsString());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        getFlowConstructStatistics().clear();
        super.doTearDown();
    }

    @Test
    public void testCorrectAverageQueueSize() throws Exception
    {
        FlowConstructStatistics stats = getFlowConstructStatistics();
        assertEquals(1, stats.getAverageQueueSize());
    }

    @Test
    public void testCorrectAsynchEventsReceived() throws Exception
    {
        FlowConstructStatistics fstats = getFlowConstructStatistics();
        assertEquals(2, fstats.getAsyncEventsReceived());
        ApplicationStatistics astats = getApplicationStatistics();
        assertEquals(2, astats.getAsyncEventsReceived());
    }

    //@Test
    //public void testCorrectMaxQueueSize() throws Exception
    //{
    //    FlowConstructStatistics stats = getFlowConstructStatistics();
    //    assertEquals(1, stats.getMaxQueueSize());
    //}
    //
    //@Test
    //public void testCorrectAsynchEventsSent() throws Exception
    //{
    //    FlowConstructStatistics stats = getFlowConstructStatistics();
    //    assertEquals(1, stats.getAsyncEventsSent());
    //}
    //
    //@Test
    //public void testCorrectTotalEventsSent() throws Exception
    //{
    //    FlowConstructStatistics stats = getFlowConstructStatistics();
    //    assertEquals(1, stats.getTotalEventsSent());
    //}

    @Test
    public void testCorrectTotalEventsReceived() throws Exception
    {
        FlowConstructStatistics stats = getFlowConstructStatistics();
        assertEquals(2, stats.getTotalEventsReceived());
        ApplicationStatistics astats = getApplicationStatistics();
        assertEquals(2, astats.getTotalEventsReceived());
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
