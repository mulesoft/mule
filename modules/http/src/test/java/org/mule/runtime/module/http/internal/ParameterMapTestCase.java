/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.service.http.api.domain.ParameterMap;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;
import java.util.HashMap;

import org.junit.Test;

@SmallTest
public class ParameterMapTestCase extends AbstractMuleTestCase {

  public static final String KEY_1 = "key1";
  public static final String KEY_2 = "key2";
  public static final String KEY_3 = "key3";
  public static final String VALUE_1 = "value1";
  public static final String VALUE_2 = "value2";

  protected ParameterMap parameterMap = getParameterMap();

  protected ParameterMap getParameterMap() {
    return new ParameterMap();
  }

  @Test
  public void putAndGet() {
    assertThat(parameterMap.put(KEY_1, VALUE_1), nullValue());
    assertThat(parameterMap.get(KEY_1), is(VALUE_1));
    assertThat(parameterMap.getAll(KEY_1), is(asList(VALUE_1)));
  }

  @Test
  public void secondPutReplaceOldValue() {
    parameterMap.put(KEY_1, VALUE_1);
    assertThat(parameterMap.put(KEY_1, VALUE_2), is(VALUE_1));
    assertThat(parameterMap.get(KEY_1), is(VALUE_2));
    assertThat(parameterMap.getAll(KEY_1), is(asList(VALUE_1, VALUE_2)));
  }

  @Test
  public void emptyMapKeySet() {
    assertThat(parameterMap.keySet(), notNullValue());
    assertThat(parameterMap.keySet(), hasSize(0));
  }

  @Test
  public void keySetReturnAllKeys() {
    parameterMap.put(KEY_1, VALUE_1);
    parameterMap.put(KEY_2, VALUE_1);
    parameterMap.put(KEY_3, VALUE_1);
    assertThat(parameterMap.keySet(), containsInAnyOrder(KEY_1, KEY_2, KEY_3));
  }

  @Test
  public void emptyValuesKeySet() {
    assertThat(parameterMap.values(), notNullValue());
    assertThat(parameterMap.values(), hasSize(0));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnPut() {
    parameterMap.toImmutableParameterMap().put(KEY_1, VALUE_1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnPutAll() {
    HashMap<String, String> map = new HashMap<>();
    map.put(KEY_1, VALUE_1);
    parameterMap.toImmutableParameterMap().putAll(map);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnRemove() {
    parameterMap.toImmutableParameterMap().remove(KEY_1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnClear() {
    parameterMap.toImmutableParameterMap().clear();
  }

  @Test
  public void valuesReturnsOnlyFirstValue() {
    parameterMap.put(KEY_1, VALUE_1);
    parameterMap.put(KEY_2, VALUE_2);
    parameterMap.put(KEY_3, VALUE_1);
    parameterMap.put(KEY_3, VALUE_2);

    Collection<String> values = parameterMap.values();
    assertThat(values, hasSize(3));
    assertThat(values, containsInAnyOrder(VALUE_1, VALUE_2, VALUE_2));
  }

  @Test
  public void toListMapValueWithEmptyMap() {
    assertThat(parameterMap.toListValuesMap().size(), is(0));
  }

  @Test
  public void toListMapValueWithSingleValues() {
    parameterMap.put(KEY_1, VALUE_1);
    parameterMap.put(KEY_2, VALUE_2);
    assertThat(parameterMap.toListValuesMap().get(KEY_1), hasItems(VALUE_1));
    assertThat(parameterMap.toListValuesMap().get(KEY_2), hasItems(VALUE_2));
  }

  @Test
  public void toListMapValueWithSeveralValues() {
    parameterMap.put(KEY_1, VALUE_1);
    parameterMap.put(KEY_1, VALUE_2);
    parameterMap.put(KEY_2, VALUE_1);
    parameterMap.put(KEY_2, VALUE_2);
    assertThat(parameterMap.toListValuesMap().get(KEY_1), hasItems(VALUE_1, VALUE_2));
    assertThat(parameterMap.toListValuesMap().get(KEY_2), hasItems(VALUE_1, VALUE_2));
  }

}
