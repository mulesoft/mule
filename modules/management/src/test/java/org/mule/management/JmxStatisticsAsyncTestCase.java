/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.management;

import org.mule.api.MuleMessage;
import org.mule.management.stats.AbstractFlowConstructStatistics;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.management.stats.ServiceStatistics;
import org.mule.module.client.MuleClient;
import org.mule.tck.FunctionalTestCase;

import java.util.Iterator;

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
        assertNotNull(response);
        assertEquals("Flow data", response.getPayloadAsString());
    }

    @Override
    protected void doTearDown() throws Exception
    {
        getServiceStatistics().clear();
        super.doTearDown();
    }

    public void testCorrectAverageQueueSize() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAverageQueueSize());
    }

    public void testCorrectAsynchEventsReceived() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAsyncEventsReceived());
        FlowConstructStatistics fstats = getFlowConstructStatistics();
        assertEquals(1, fstats.getAsyncEventsReceived());
    }

    public void testCorrectMaxQueueSize() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getMaxQueueSize());
    }

    public void testCorrectAsynchEventsSent() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getAsyncEventsSent());
    }

    public void testCorrectTotalEventsSent() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getTotalEventsSent());
    }

    public void testCorrectTotalEventsReceived() throws Exception
    {
        ServiceStatistics stats = getServiceStatistics();
        assertEquals(1, stats.getTotalEventsReceived());
        FlowConstructStatistics fstats = getFlowConstructStatistics();
        assertEquals(1, fstats.getTotalEventsReceived());
    }

    private ServiceStatistics getServiceStatistics()
    {
        Iterator<FlowConstructStatistics> iterator = muleContext.getStatistics().getServiceStatistics().iterator();
        AbstractFlowConstructStatistics stat1 = iterator.next();
        if (stat1 instanceof ServiceStatistics)
        {
            return (ServiceStatistics)stat1;
        }
        else
        {
            return (ServiceStatistics)iterator.next();
        }
    }

    private FlowConstructStatistics getFlowConstructStatistics()
    {
        Iterator<FlowConstructStatistics> iterator = muleContext.getStatistics().getServiceStatistics().iterator();
        AbstractFlowConstructStatistics stat1 = iterator.next();
        if (stat1 instanceof FlowConstructStatistics)
        {
            return (FlowConstructStatistics)stat1;
        }
        else
        {
            return (FlowConstructStatistics)iterator.next();
        }
    }
}
