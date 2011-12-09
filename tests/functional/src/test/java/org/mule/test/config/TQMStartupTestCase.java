/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import org.junit.Test;

import org.mule.api.MuleContext;
import org.mule.api.MuleException;
import org.mule.api.construct.FlowConstruct;
import org.mule.api.exception.MessagingExceptionHandler;
import org.mule.api.lifecycle.LifecycleException;
import org.mule.api.lifecycle.LifecycleManager;
import org.mule.api.lifecycle.LifecycleState;
import org.mule.api.lifecycle.Startable;
import org.mule.api.lifecycle.Stoppable;
import org.mule.api.processor.MessageProcessorChain;
import org.mule.api.routing.MessageInfoMapping;
import org.mule.construct.SimpleFlowConstruct;
import org.mule.lifecycle.DefaultLifecycleState;
import org.mule.management.stats.FlowConstructStatistics;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.util.queue.QueuePersistenceStrategy;
import org.mule.util.queue.TransactionalQueueManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

public class TQMStartupTestCase extends AbstractMuleContextTestCase
{
    List<Object> startStopOrder = new ArrayList<Object>();
    @Test
    public void testStartupOrder() throws Exception
    {
        TransactionalQueueManager tqm = (TransactionalQueueManager)muleContext.getQueueManager();
        PersistenceStrategy ps = new PersistenceStrategy();
        tqm.setPersistenceStrategy(ps);
        FlowConstruct fc = new RecordingFlowConstruct("dummy", muleContext);
        muleContext.getRegistry().registerFlowConstruct(fc);
        muleContext.start();
        muleContext.stop();
        assertEquals(4, startStopOrder.size());
        assertSame(ps, startStopOrder.get(0));
        assertSame(fc, startStopOrder.get(1));
        assertSame(fc, startStopOrder.get(2));
        assertSame(ps, startStopOrder.get(3));
    }

    private class PersistenceStrategy implements QueuePersistenceStrategy
    {
        public Object store(String queue, Object obj) throws IOException
        {
            return null;
        }

        public Object load(String queue, Object id) throws IOException
        {
            return null;
        }

        public void remove(String queue, Object id) throws IOException
        {
        }

        public List restore() throws IOException
        {
            return new ArrayList();
        }

        public void open() throws IOException
        {
            startStopOrder.add(this);
        }

        public void close() throws IOException
        {
            startStopOrder.add(this);
        }

        public boolean isTransient()
        {
            return false;
        }
    }

    private class RecordingFlowConstruct extends SimpleFlowConstruct
    {
        public RecordingFlowConstruct(String name, MuleContext muleContext)
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
}
