/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import org.mule.api.config.MuleProperties;
import org.mule.api.lifecycle.InitialisationException;
import org.mule.api.schedule.SchedulerFactoryPostProcessor;
import org.mule.api.store.ObjectStore;
import org.mule.api.store.ObjectStoreManager;
import org.mule.config.spring.factories.WatermarkFactoryBean;
import org.mule.api.MuleEvent;
import org.mule.api.MuleException;
import org.mule.api.processor.MessageProcessor;
import org.mule.api.schedule.Scheduler;
import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.transport.AbstractPollingMessageReceiver;
import org.mule.transport.polling.MessageProcessorPollingConnector;
import org.mule.transport.polling.PollingMessageSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

public class PollingTestCase extends FunctionalTestCase
{

    @Override
    protected String getConfigResources()
    {
        return "org/mule/test/integration/polling-config.xml";
    }


    /**
     * Watermark: No object store, no update expression, no object store key present in the object store.
     * Poll with watermark but no Object Stored Defined, and unexistent object store key. It executes the default value
     * and register that values into the flow var with the name of the key. Store the same value in the object store as
     * it has no update expression.
     */
    @Test
    public void pollWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("nameNotDefinedWatermarkObjectStoreFlow");
        Thread.sleep(3000);
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains("test1"));
        assertEquals("noKey", os.retrieve("test1"));
    }

    /**
     * Pre: No object store, no update expression, no object store key present in the object store. The user
     * changes the watermark value in the flow.
     * Pos: The key is stored in the object store with the value that the user set
     */
    @Test
    public void pollChangeKeyValueWithNoKeyInTheObjectStore() throws Exception
    {
        executePollOf("changeWatermarkWihtNotDefinedWatermarkObjectStoreFlow");
        Thread.sleep(3000);
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains("test2"));
        assertEquals("keyPresent", os.retrieve("test2"));
    }

    /**
     * Pre: No object store, no update expression. The user changes the watermark value in the flow.
     * Pos: The key is stored in the object store with the value that the user set  in the flow, the flow variable of the
     * watermark that was present in the object store can be used in the flow.
     */
    @Test
    public void pollUsingWatermark() throws Exception
    {
        getDefaultObjectStore().store("test3", "testValue");
        executePollOf("usingWatermarkFlow");
        Thread.sleep(3000);
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains("test3"));
        assertEquals("keyPresent", os.retrieve("test3"));
    }

    /**
     * Pre: The key is an expression
     * Pos: The key is evaluated twice, at the beginning of the message source and at the end of the flow
     */
    @Test
    public void watermarkWithKeyAsAnExpression() throws Exception
    {
        getDefaultObjectStore().store("test4", "testValue");
        executePollOf("watermarkWithKeyAsAnExpression");
        Thread.sleep(3000);
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains("test4"));
        assertEquals("keyPresent", os.retrieve("test4"));
    }


    @Test
    public void watermarkWithUpdateExpression() throws Exception
    {
        getDefaultObjectStore().store("test5", "testValue");
        executePollOf("watermarkWithUpdateExpression");
        Thread.sleep(3000);
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains("test5"));
        assertEquals("valueUpdated", os.retrieve("test5"));
    }

    @Test
    public void watermarkWithAnnotations() throws Exception
    {
        getDefaultObjectStore().store("test", "testValue");
        executePollOf("watermarkWithAnnotations");
        Thread.sleep(3000);
        ObjectStore os = getDefaultObjectStore();
        assertTrue(os.contains("test6"));
        assertEquals("keyPresent", os.retrieve("test6"));
    }

    private ObjectStore getDefaultObjectStore()
    {
        ObjectStoreManager mgr = (ObjectStoreManager) muleContext.getRegistry().get(MuleProperties.OBJECT_STORE_MANAGER);
        return mgr.getObjectStore(WatermarkFactoryBean.MULE_WATERMARK_PARTITION);
    }

    private void executePollOf(String flowName) throws Exception
    {
        Scheduler scheduler = muleContext.getRegistry().lookupScheduler(PollingMessageSource.flowPollingSchedulers(flowName)).iterator().next();
        scheduler.schedule();
    }


    public static class PollStopper implements SchedulerFactoryPostProcessor{

        @Override
        public Scheduler process(final Scheduler scheduler)
        {
            return new Scheduler()
            {
                @Override
                public void schedule() throws Exception
                {
                    scheduler.schedule();
                }

                @Override
                public void dispose()
                {
                    scheduler.dispose();
                }

                @Override
                public void initialise() throws InitialisationException
                {
                    scheduler.initialise();
                }

                @Override
                public void setName(String name)
                {
                    scheduler.setName(name);
                }

                @Override
                public String getName()
                {
                    return scheduler.getName();
                }

                @Override
                public void start() throws MuleException
                {
                    // Does nothing
                }

                @Override
                public void stop() throws MuleException
                {
                    // Does Nothing
                }
            };
        }
    }
  }