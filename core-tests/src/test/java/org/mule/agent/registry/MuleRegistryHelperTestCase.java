/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.agent.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleException;
import org.mule.api.registry.MuleRegistry;
import org.mule.api.schedule.Scheduler;
import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;
import org.mule.util.Predicate;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MuleRegistryHelperTestCase extends AbstractMuleContextTestCase
{

    private Transformer t1;
    private Transformer t2;

    @Before
    public void setUp() throws Exception
    {
        t1 = new MockConverterBuilder().named("t1").from(DataTypeFactory.create(Orange.class)).to(DataTypeFactory.create(Fruit.class)).build();
        muleContext.getRegistry().registerTransformer(t1);

        t2 = new MockConverterBuilder().named("t2").from(DataTypeFactory.OBJECT).to(DataTypeFactory.create(Fruit.class)).build();
        muleContext.getRegistry().registerTransformer(t2);
    }

    @Test
    public void lookupsTransformersByType() throws Exception
    {
        List trans =  muleContext.getRegistry().lookupTransformers(new SimpleDataType(BloodOrange.class), new SimpleDataType(Fruit.class));
        assertEquals(2, trans.size());
        assertTrue(trans.contains(t1));
        assertTrue(trans.contains(t2));
    }

    @Test
    public void lookupsTransformerByPriority() throws Exception
    {
        Transformer result =  muleContext.getRegistry().lookupTransformer(new SimpleDataType(BloodOrange.class), new SimpleDataType(Fruit.class));
        assertNotNull(result);
        assertEquals(t1, result);
    }

    @Test
    public void registerScheduler() throws MuleException
    {
        Scheduler scheduler = scheduler();
        register(scheduler);
        muleContext.getRegistry().unregisterScheduler(scheduler);
        assertNull(muleContext.getRegistry().lookupObject("schedulerName"));
    }

    @Test
    public void lookupScheduler() throws MuleException
    {
        Scheduler scheduler = scheduler();
        register(scheduler);
        assertEquals(scheduler, muleContext.getRegistry().lookupScheduler(new Predicate<String>()
        {
            @Override
            public boolean evaluate(String s)
            {
                return s.equalsIgnoreCase("schedulerName");
            }
        }).iterator().next());
    }

    @Test
    public void unregisterScheduler() throws MuleException
    {
        Scheduler scheduler = scheduler();
        register(scheduler);

        assertEquals(scheduler, muleContext.getRegistry().lookupObject("schedulerName"));
    }

    private void register(Scheduler scheduler) throws MuleException
    {
        muleContext.getRegistry().registerScheduler(scheduler);
    }

    private Scheduler scheduler()
    {
        Scheduler scheduler = mock(Scheduler.class);
        when(scheduler.getName()).thenReturn("schedulerName");
        return scheduler;
    }
}
