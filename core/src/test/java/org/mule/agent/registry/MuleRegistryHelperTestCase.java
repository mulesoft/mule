/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.agent.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.api.transformer.Transformer;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.transformer.builder.MockConverterBuilder;
import org.mule.transformer.types.DataTypeFactory;
import org.mule.transformer.types.SimpleDataType;

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
}
