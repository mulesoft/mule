/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transformer.graph;

import static java.util.Arrays.asList;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.mule.runtime.api.metadata.DataType.JSON_STRING;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.transformer.builder.MockConverterBuilder;
import org.mule.tck.size.SmallTest;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.tck.testmodels.fruit.Banana;
import org.mule.tck.testmodels.fruit.Orange;
import org.mule.tck.testmodels.fruit.Peach;
import org.mule.tck.testmodels.fruit.Seed;

import java.util.List;

import org.junit.Test;

@SmallTest
public class TypeMatchingVertexesFilterTestCase {

  private static final DataType ORANGE_DATA_TYPE = DataType.fromType(Orange.class);

  private static final DataType PEACH_DATA_TYPE = DataType.fromType(Peach.class);
  private static final DataType SEED_DATA_TYPE = DataType.fromType(Seed.class);
  private static final DataType APPLE_DATA_TYPE = DataType.fromType(Apple.class);
  private static final DataType BANANA_DATA_TYPE = DataType.fromType(Banana.class);

  private static final ConverterFilter filter = new TypeMatchingVertexesFilter();


  @Test
  public void sameListIsReturnedIfNoMatch() throws Exception {

    Converter stringToOrange = new MockConverterBuilder().named("stringToOrange").from(STRING).to(ORANGE_DATA_TYPE).build();
    Converter bananaToApple =
        new MockConverterBuilder().named("bananaToApple").from(BANANA_DATA_TYPE).to(APPLE_DATA_TYPE).build();

    List<Converter> converters = asList(stringToOrange, bananaToApple);

    List<Converter> filtered = filter.filter(converters, PEACH_DATA_TYPE, SEED_DATA_TYPE);

    assertThat(filtered, is(equalTo(converters)));

  }

  @Test
  public void matchingSourceTypeConvertersAreReturned() throws Exception {

    Converter stringToOrange = new MockConverterBuilder().named("stringToOrange").from(STRING).to(ORANGE_DATA_TYPE).build();
    Converter bananaToApple =
        new MockConverterBuilder().named("bananaToApple").from(BANANA_DATA_TYPE).to(APPLE_DATA_TYPE).build();
    Converter stringToSeed = new MockConverterBuilder().named("stringToSeed").from(STRING).to(SEED_DATA_TYPE).build();

    List<Converter> converters = asList(stringToOrange, bananaToApple, stringToSeed);

    List<Converter> filtered = filter.filter(converters, STRING, PEACH_DATA_TYPE);

    assertThat(filtered, hasSize(2));
    assertThat(filtered, contains(stringToOrange, stringToSeed));

  }

  @Test
  public void matchingSourceTypeConvertersAreReturnedEvenWithSuperTypesPresent() throws Exception {

    Converter stringToOrange = new MockConverterBuilder().named("stringToOrange").from(STRING).to(ORANGE_DATA_TYPE).build();
    Converter objectToApple = new MockConverterBuilder().named("objectToApple").from(OBJECT).to(APPLE_DATA_TYPE).build();
    Converter stringToSeed = new MockConverterBuilder().named("stringToSeed").from(STRING).to(SEED_DATA_TYPE).build();

    List<Converter> converters = asList(stringToOrange, objectToApple, stringToSeed);

    List<Converter> filtered = filter.filter(converters, STRING, PEACH_DATA_TYPE);

    assertThat(filtered, hasSize(2));
    assertThat(filtered, contains(stringToOrange, stringToSeed));

  }

  @Test
  public void matchingResultTypeConvertersAreReturned() throws Exception {

    Converter stringToOrange = new MockConverterBuilder().named("stringToOrange").from(STRING).to(ORANGE_DATA_TYPE).build();
    Converter objectToApple = new MockConverterBuilder().named("objectToApple").from(OBJECT).to(APPLE_DATA_TYPE).build();
    Converter stringToSeed = new MockConverterBuilder().named("stringToSeed").from(STRING).to(SEED_DATA_TYPE).build();

    List<Converter> converters = asList(stringToOrange, objectToApple, stringToSeed);

    List<Converter> filtered = filter.filter(converters, SEED_DATA_TYPE, ORANGE_DATA_TYPE);

    assertThat(filtered, hasSize(1));
    assertThat(filtered, contains(stringToOrange));

  }

  @Test
  public void bothVertexesMatchingIsBetterThanJustOne() throws Exception {

    Converter stringToOrange = new MockConverterBuilder().named("stringToOrange").from(STRING).to(ORANGE_DATA_TYPE).build();
    Converter stringToBanana = new MockConverterBuilder().named("stringToBanana").from(STRING).to(BANANA_DATA_TYPE).build();
    Converter seedToOrange = new MockConverterBuilder().named("seedToOrange").from(SEED_DATA_TYPE).to(ORANGE_DATA_TYPE).build();

    List<Converter> converters = asList(stringToOrange, stringToBanana, seedToOrange);

    List<Converter> filtered = filter.filter(converters, STRING, ORANGE_DATA_TYPE);

    assertThat(filtered, hasSize(1));
    assertThat(filtered, contains(stringToOrange));

  }

  @Test
  public void resultMimeTypeIsFiltered() throws Exception {

    Converter bananaToString = new MockConverterBuilder().named("BananaToString").from(BANANA_DATA_TYPE).to(STRING).build();
    Converter bananaToJsonString =
        new MockConverterBuilder().named("BananaToJsonString").from(BANANA_DATA_TYPE).to(JSON_STRING).build();

    List<Converter> converters = asList(bananaToString, bananaToJsonString);

    List<Converter> filtered = filter.filter(converters, BANANA_DATA_TYPE, JSON_STRING);

    assertThat(filtered, hasSize(1));
    assertThat(filtered, contains(bananaToJsonString));
  }

}
