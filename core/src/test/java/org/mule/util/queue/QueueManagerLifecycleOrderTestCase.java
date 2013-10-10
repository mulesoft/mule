/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.util.queue;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.config.MuleProperties;
import org.mule.api.construct.FlowConstruct;
import org.mule.config.builders.DefaultsConfigurationBuilder;
import org.mule.construct.Flow;
import org.mule.context.DefaultMuleContextFactory;
import org.mule.security.MuleSecurityManager;
import org.mule.util.xa.ResourceManagerSystemException;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class QueueManagerLifecycleOrderTestCase
{
    private List<Object> startStopOrder = new ArrayList<Object>();
    private RecordingTQM rtqm = new RecordingTQM();

    @Test
    public void testStartupOrder() throws Exception
    {
        MuleContext muleContext = new DefaultMuleContextFactory().createMuleContext(new QueueManagerOnlyConfigurationBuilder());
        FlowConstruct fc = new RecordingFlow("dummy", muleContext);
        muleContext.getRegistry().registerFlowConstruct(fc);
        muleContext.start();
        muleContext.stop();
        assertEquals(4, startStopOrder.size());
        assertSame(rtqm, startStopOrder.get(0));
        assertSame(fc, startStopOrder.get(1));
        assertSame(fc, startStopOrder.get(2));
        assertSame(rtqm, startStopOrder.get(3));
    }

    private class RecordingTQM extends TransactionalQueueManager
    {
        @Override
        public void start() throws ResourceManagerSystemException
        {
            startStopOrder.add(this);
            super.start();
        }

        @Override
        public void stop() throws ResourceManagerSystemException
        {
            startStopOrder.add(this);
            super.stop();
        }
    }

    private class RecordingFlow extends Flow
    {
        public RecordingFlow(String name, MuleContext muleContext)
        {
            super(name, muleContext);
        }

        public void doStart() throws MuleException
        {
            startStopOrder.add(this);
        }

        public void doStop() throws MuleException
        {
            startStopOrder.add(this);
        }
    }

    private class QueueManagerOnlyConfigurationBuilder extends DefaultsConfigurationBuilder
    {
        @Override
        protected void doConfigure(MuleContext muleContext) throws Exception
        {
            muleContext.getRegistry().registerObject(MuleProperties.OBJECT_QUEUE_MANAGER, rtqm);
            muleContext.getRegistry().registerObject(MuleProperties.OBJECT_SECURITY_MANAGER,
                new MuleSecurityManager());

        }
    }
}
