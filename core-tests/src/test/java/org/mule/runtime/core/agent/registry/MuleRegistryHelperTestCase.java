/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.agent.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.tck.testmodels.fruit.BloodOrange;
import org.mule.tck.testmodels.fruit.Fruit;
import org.mule.tck.testmodels.fruit.Orange;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MuleRegistryHelperTestCase extends AbstractMuleContextTestCase {

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);
  private static final DataType BLOOD_ORANGE_DATA_TYPE = DataType.fromType(BloodOrange.class);
  private static final DataType FRUIT_DATA_TYPE = DataType.fromType(Fruit.class);

  private Transformer t1;
  private Transformer t2;

  @Before
  public void setUp() throws Exception {
    t1 = new MockConverterBuilder().named("t1").from(ORANGE_DATA_TYPE).to(FRUIT_DATA_TYPE).build();
    muleContext.getRegistry().registerTransformer(t1);

    t2 = new MockConverterBuilder().named("t2").from(DataType.OBJECT).to(FRUIT_DATA_TYPE).build();
    muleContext.getRegistry().registerTransformer(t2);
  }

  @Test
  public void lookupsTransformersByType() throws Exception {
    List trans = muleContext.getRegistry().lookupTransformers(BLOOD_ORANGE_DATA_TYPE, FRUIT_DATA_TYPE);
    assertEquals(2, trans.size());
    assertTrue(trans.contains(t1));
    assertTrue(trans.contains(t2));
  }

  @Test
  public void lookupsTransformerByPriority() throws Exception {
    Transformer result = muleContext.getRegistry().lookupTransformer(BLOOD_ORANGE_DATA_TYPE, FRUIT_DATA_TYPE);
    assertNotNull(result);
    assertEquals(t1, result);
  }
}
