/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.quartz;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.api.MuleContext;
import org.mule.api.transport.Connector;
import org.mule.transport.AbstractConnectorTestCase;

import org.junit.Test;
import org.mockito.InOrder;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;

public class QuartzConnectorTestCase extends AbstractConnectorTestCase
{

    private final SchedulerFactory schedulerFactory = mock(SchedulerFactory.class);
    private final Scheduler scheduler = mock(Scheduler.class, RETURNS_DEEP_STUBS);

    @Override
    public Connector createConnector() throws Exception
    {
        QuartzConnector quartzConnector = new TestQuartzConnector(muleContext);
        quartzConnector.setName("QuartzConnector");
        return quartzConnector;
    }

    @Override
    public Object getValidMessage() throws Exception
    {
        return "test";
    }

    @Test
    public void restartConnector() throws Exception
    {
        getConnector().start();
        getConnector().stop();
        getConnector().start();
        InOrder inOrder = inOrder(scheduler);
        inOrder.verify(scheduler).start();
        inOrder.verify(scheduler).standby();
        inOrder.verify(scheduler).start();
    }

    @Test
    public void testStopConnector() throws Exception
    {
        getConnector().start();
        getConnector().stop();
        verify(scheduler).standby();
        verify(scheduler, never()).shutdown();
    }

    private class TestQuartzConnector extends QuartzConnector
    {
        public TestQuartzConnector(MuleContext context)
        {
            super(context);
        }

        @Override
        protected SchedulerFactory createSchedulerFactory() throws SchedulerException
        {
            when(schedulerFactory.getScheduler()).thenReturn(scheduler);
            return schedulerFactory;
        }
    }

    @Override
    public String getTestEndpointURI()
    {
        return "quartz:/myService?repeatInterval=1000";
    }
}
