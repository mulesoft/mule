/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.registry.TransformerWeighting;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.FruitBowl;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.Serializable;

@SmallTest
public class TransformerWeightingTestCase extends AbstractMuleTestCase {

  @Test
  public void testExactMatch() throws Exception {
    Transformer trans = new MockConverterBuilder().from(DataType.fromType(IOException.class)).to(DataType.BYTE_ARRAY).build();

    TransformerWeighting weighting = new TransformerWeighting(IOException.class, byte[].class, trans);

    assertFalse(weighting.isNotMatch());
    assertTrue(weighting.isExactMatch());
  }

  @Test
  public void testNearMatch() throws Exception {
    Transformer trans = new MockConverterBuilder().from(DataType.INPUT_STREAM).to(DataType.BYTE_ARRAY).build();

    TransformerWeighting weighting = new TransformerWeighting(FilterInputStream.class, byte[].class, trans);

    assertFalse(weighting.isNotMatch());
    assertFalse(weighting.isExactMatch());
    assertEquals(1, weighting.getInputWeighting());
    assertEquals(0, weighting.getOutputWeighting());
  }

  @Test
  public void testNoMatchWeighting() throws Exception {
    Transformer trans = new MockConverterBuilder().from(DataType.fromType(Serializable.class)).to(DataType.BYTE_ARRAY).build();

    TransformerWeighting weighting = new TransformerWeighting(FruitBowl.class, byte[].class, trans);

    assertTrue(weighting.isNotMatch());
    assertEquals(-1, weighting.getInputWeighting());
    assertEquals(0, weighting.getOutputWeighting());
  }

  @Test
  public void testCompareWeightingWithNearMatches() throws Exception {
    Transformer trans1 = new MockConverterBuilder().from(DataType.fromType(Serializable.class)).to(DataType.BYTE_ARRAY).build();
    Transformer trans2 = new MockConverterBuilder().from(DataType.fromType(Exception.class)).to(DataType.BYTE_ARRAY).build();

    TransformerWeighting weighting1 = new TransformerWeighting(IOException.class, byte[].class, trans1);
    TransformerWeighting weighting2 = new TransformerWeighting(IOException.class, byte[].class, trans2);

    assertFalse(weighting1.isNotMatch());
    assertFalse(weighting2.isNotMatch());
    assertFalse(weighting1.isExactMatch());
    assertFalse(weighting2.isExactMatch());
    // Weighting2 two is a better match
    assertEquals(1, weighting2.compareTo(weighting1));

    assertEquals(3, weighting1.getInputWeighting());
    assertEquals(1, weighting2.getInputWeighting());
    assertEquals(0, weighting1.getOutputWeighting());
    assertEquals(0, weighting2.getOutputWeighting());
  }

  @Test
  public void testCompareWeightingWithExactMatch() throws Exception {
    Transformer trans1 = new MockConverterBuilder().from(DataType.fromType(Serializable.class)).to(DataType.BYTE_ARRAY).build();
    Transformer trans2 = new MockConverterBuilder().from(DataType.fromType(IOException.class)).to(DataType.BYTE_ARRAY).build();

    TransformerWeighting weighting1 = new TransformerWeighting(IOException.class, byte[].class, trans1);
    TransformerWeighting weighting2 = new TransformerWeighting(IOException.class, byte[].class, trans2);

    assertFalse(weighting1.isNotMatch());
    assertFalse(weighting2.isNotMatch());
    assertFalse(weighting1.isExactMatch());
    assertTrue(weighting2.isExactMatch());
    // Weighting2 two is an exact match
    assertEquals(1, weighting2.compareTo(weighting1));

    assertEquals(3, weighting1.getInputWeighting());
    assertEquals(0, weighting2.getInputWeighting());
    assertEquals(0, weighting1.getOutputWeighting());
    assertEquals(0, weighting2.getOutputWeighting());
  }

  @Test
  public void testOrderInNoMatchWhenDifferenceBetweenInputAndOutputWeightings() {
    Transformer trans1 =
        new MockConverterBuilder().from(DataType.fromType(IOException.class)).to(DataType.fromType(IOException.class)).build();
    Transformer trans2 =
        new MockConverterBuilder().from(DataType.fromType(IOException.class)).to(DataType.fromType(IOException.class)).build();

    TransformerWeighting weighting1 = new TransformerWeighting(byte[].class, IOException.class, trans1);
    TransformerWeighting weighting2 = new TransformerWeighting(IOException.class, byte[].class, trans2);

    assertThat(weighting1.isNotMatch(), equalTo(true));
    assertThat(weighting2.isNotMatch(), equalTo(true));

    assertThat(weighting1.compareTo(weighting2), equalTo(-1));
    assertThat(weighting2.compareTo(weighting1), equalTo(1));
  }

  @Test
  public void testCompareWeightingWithNoMatch() throws Exception {
    Transformer trans1 = new MockConverterBuilder().from(DataType.fromType(Serializable.class)).to(DataType.BYTE_ARRAY).build();
    Transformer trans2 = new MockConverterBuilder().from(DataType.fromType(FruitBowl.class)).to(DataType.BYTE_ARRAY).build();

    TransformerWeighting weighting1 = new TransformerWeighting(IOException.class, byte[].class, trans1);
    TransformerWeighting weighting2 = new TransformerWeighting(IOException.class, byte[].class, trans2);

    assertFalse(weighting1.isNotMatch());
    assertTrue(weighting2.isNotMatch());
    assertFalse(weighting1.isExactMatch());
    assertFalse(weighting2.isExactMatch());
    // Weighting2 two is not a match
    assertEquals(-1, weighting2.compareTo(weighting1));

    assertEquals(3, weighting1.getInputWeighting());
    assertEquals(-1, weighting2.getInputWeighting());
    assertEquals(0, weighting1.getOutputWeighting());
    assertEquals(0, weighting2.getOutputWeighting());
  }

  @Test
  public void testComparableContractHonored() {
    Transformer trans1 =
        new MockConverterBuilder().from(DataType.fromType(Object.class)).to(DataType.fromType(byte[].class)).build();
    Transformer trans2 =
        new MockConverterBuilder().from(DataType.fromType(FruitBowl.class)).to(DataType.fromType(String.class)).build();

    TransformerWeighting weighting1 = new TransformerWeighting(ByteArrayInputStream.class, String.class, trans1);
    TransformerWeighting weighting2 = new TransformerWeighting(ByteArrayInputStream.class, String.class, trans2);

    assertThat(weighting1.compareTo(weighting2), equalTo(1));
    assertThat(weighting2.compareTo(weighting1), equalTo(-1));

    assertThat(weighting1.compareTo(weighting1), equalTo(0));
    assertThat(weighting2.compareTo(weighting2), equalTo(0));
  }
}
