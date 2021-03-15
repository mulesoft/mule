/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.registry;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.runtime.core.privileged.transformer.CompositeConverter;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.Peach;
import org.mule.tck.testmodels.fruit.Seed;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class MuleRegistryHelperTestCase extends AbstractMuleContextTestCase {

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);
  private static final DataType BLOOD_ORANGE_DATA_TYPE = DataType.fromType(BloodOrange.class);
  private static final DataType FRUIT_DATA_TYPE = DataType.fromType(Fruit.class);

  private static final DataType PEACH_DATA_TYPE = DataType.fromType(Peach.class);
  private static final DataType SEED_DATA_TYPE = DataType.fromType(Seed.class);
  private static final DataType APPLE_DATA_TYPE = DataType.fromType(Apple.class);
  private static final DataType BANANA_DATA_TYPE = DataType.fromType(Banana.class);

  private Transformer t1;
  private Transformer t2;

  @Before
  public void setUp() throws Exception {
    t1 = new MockConverterBuilder().named("t1").from(ORANGE_DATA_TYPE).to(FRUIT_DATA_TYPE).build();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(t1);

    t2 = new MockConverterBuilder().named("t2").from(DataType.OBJECT).to(FRUIT_DATA_TYPE).build();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(t2);
  }

  @Test
  public void lookupsTransformersByType() throws Exception {
    List trans =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformers(BLOOD_ORANGE_DATA_TYPE, FRUIT_DATA_TYPE);
    assertEquals(2, trans.size());
    assertTrue(trans.contains(t1));
    assertTrue(trans.contains(t2));
  }

  @Test
  public void lookupsTransformerByPriority() throws Exception {
    Transformer result =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformer(BLOOD_ORANGE_DATA_TYPE, FRUIT_DATA_TYPE);
    assertNotNull(result);
    assertEquals(t1, result);
  }

  @Test
  public void findsCompositeTransformerEvenIfDirectNotFound() throws Exception {
    Transformer fruitToSeed = new MockConverterBuilder().named("fruitToSeed").from(FRUIT_DATA_TYPE).to(SEED_DATA_TYPE).build();
    Transformer seedToApple = new MockConverterBuilder().named("seedToApple").from(SEED_DATA_TYPE).to(APPLE_DATA_TYPE).build();
    Transformer appleToBanana =
        new MockConverterBuilder().named("appleToBanana").from(APPLE_DATA_TYPE).to(BANANA_DATA_TYPE).build();
    Transformer bananaToBloodOrange =
        new MockConverterBuilder().named("bananaToBloodOrange").from(BANANA_DATA_TYPE).to(BLOOD_ORANGE_DATA_TYPE).build();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(fruitToSeed);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(seedToApple);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(appleToBanana);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(bananaToBloodOrange);

    Transformer trans =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformer(FRUIT_DATA_TYPE, BLOOD_ORANGE_DATA_TYPE);
    assertThat(trans, is(notNullValue()));
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));

    // The same should be returned if we ask for it with compatible data types
    trans = ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformer(FRUIT_DATA_TYPE, ORANGE_DATA_TYPE);
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));

    trans = ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformer(PEACH_DATA_TYPE, BLOOD_ORANGE_DATA_TYPE);
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));

    trans = ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformer(PEACH_DATA_TYPE, ORANGE_DATA_TYPE);
    assertThat(trans, instanceOf(CompositeConverter.class));
    assertThat(trans.getName(), is("fruitToSeedseedToAppleappleToBananabananaToBloodOrange"));
  }

  @Test
  public void closestToTypesTransformerIsFoundEvenIfWeightIsLess() throws Exception {
    Transformer bananaToBloodOrange = new MockConverterBuilder().named("bananaToBloodOrange").from(BANANA_DATA_TYPE)
        .to(BLOOD_ORANGE_DATA_TYPE).weighting(10).build();
    Transformer bananaToOrange =
        new MockConverterBuilder().named("bananaToOrange").from(BANANA_DATA_TYPE).to(ORANGE_DATA_TYPE).weighting(1).build();
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(bananaToBloodOrange);
    ((MuleContextWithRegistry) muleContext).getRegistry().registerTransformer(bananaToOrange);

    Transformer trans =
        ((MuleContextWithRegistry) muleContext).getRegistry().lookupTransformer(BANANA_DATA_TYPE, ORANGE_DATA_TYPE);

    assertThat(trans, is(notNullValue()));
    assertThat(trans.getName(), is("bananaToOrange"));
  }

}
