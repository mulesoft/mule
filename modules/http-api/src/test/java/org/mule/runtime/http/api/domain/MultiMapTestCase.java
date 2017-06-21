/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.domain;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.MULTI_MAP;

import org.mule.runtime.api.util.MultiMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_SERVICE)
@Stories(MULTI_MAP)
public class MultiMapTestCase {

  public static final String KEY_1 = "key1";
  public static final String KEY_2 = "key2";
  public static final String KEY_3 = "key3";
  public static final String VALUE_1 = "value1";
  public static final String VALUE_2 = "value2";

  protected MultiMap<String, String> multiMap = getMultiMap();

  protected MultiMap<String, String> getMultiMap() {
    return new MultiMap<>();
  }

  @Test
  public void putAndGet() {
    assertThat(multiMap.put(KEY_1, VALUE_1), nullValue());
    assertThat(multiMap.get(KEY_1), is(VALUE_1));
    assertThat(multiMap.getAll(KEY_1), is(asList(VALUE_1)));
  }

  @Test
  public void secondPutReplaceOldValue() {
    multiMap.put(KEY_1, VALUE_1);
    assertThat(multiMap.put(KEY_1, VALUE_2), is(VALUE_1));
    assertThat(multiMap.get(KEY_1), is(VALUE_2));
    assertThat(multiMap.getAll(KEY_1), is(asList(VALUE_1, VALUE_2)));
  }

  @Test
  public void emptyMapKeySet() {
    assertThat(multiMap.keySet(), notNullValue());
    assertThat(multiMap.keySet(), hasSize(0));
  }

  @Test
  public void keySetReturnAllKeys() {
    multiMap.put(KEY_1, VALUE_1);
    multiMap.put(KEY_2, VALUE_1);
    multiMap.put(KEY_3, VALUE_1);
    assertThat(multiMap.keySet(), containsInAnyOrder(KEY_1, KEY_2, KEY_3));
  }

  @Test
  public void emptyValuesKeySet() {
    assertThat(multiMap.values(), notNullValue());
    assertThat(multiMap.values(), hasSize(0));
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnPut() {
    multiMap.toImmutableParameterMap().put(KEY_1, VALUE_1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnPutAll() {
    HashMap<String, String> map = new HashMap<>();
    map.put(KEY_1, VALUE_1);
    multiMap.toImmutableParameterMap().putAll(map);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnRemove() {
    multiMap.toImmutableParameterMap().remove(KEY_1);
  }

  @Test(expected = UnsupportedOperationException.class)
  public void immutableParameterMapFailsOnClear() {
    multiMap.toImmutableParameterMap().clear();
  }

  @Test
  public void valuesReturnsOnlyFirstValue() {
    multiMap.put(KEY_1, VALUE_1);
    multiMap.put(KEY_2, VALUE_2);
    multiMap.put(KEY_3, VALUE_1);
    multiMap.put(KEY_3, VALUE_2);

    Collection<String> values = multiMap.values();
    assertThat(values, hasSize(3));
    assertThat(values, containsInAnyOrder(VALUE_1, VALUE_2, VALUE_2));
  }

  @Test
  public void toListMapValueWithEmptyMap() {
    assertThat(multiMap.toListValuesMap().size(), is(0));
  }

  @Test
  public void toListMapValueWithSingleValues() {
    multiMap.put(KEY_1, VALUE_1);
    multiMap.put(KEY_2, VALUE_2);
    assertThat(multiMap.toListValuesMap().get(KEY_1), hasItems(VALUE_1));
    assertThat(multiMap.toListValuesMap().get(KEY_2), hasItems(VALUE_2));
  }

  @Test
  public void toListMapValueWithSeveralValues() {
    multiMap.put(KEY_1, VALUE_1);
    multiMap.put(KEY_1, VALUE_2);
    multiMap.put(KEY_2, VALUE_1);
    multiMap.put(KEY_2, VALUE_2);
    assertThat(multiMap.toListValuesMap().get(KEY_1), hasItems(VALUE_1, VALUE_2));
    assertThat(multiMap.toListValuesMap().get(KEY_2), hasItems(VALUE_1, VALUE_2));
  }

  @Test
  public void entryList() {
    multiMap.put(KEY_1, VALUE_1);
    multiMap.put(KEY_1, VALUE_2);
    multiMap.put(KEY_2, VALUE_1);
    multiMap.put(KEY_2, VALUE_2);
    assertThat(multiMap.entryList(), hasSize(4));
    assertThat(multiMap.entryList(), contains(new EntryMatcher(KEY_1, VALUE_1),
                                              new EntryMatcher(KEY_1, VALUE_2),
                                              new EntryMatcher(KEY_2, VALUE_1),
                                              new EntryMatcher(KEY_2, VALUE_2)));
  }

  private class EntryMatcher extends TypeSafeMatcher<Map.Entry<String, String>> {

    private String expectedKey;
    private String expectedValue;

    public EntryMatcher(String expectedKey, String expectedValue) {
      this.expectedKey = expectedKey;
      this.expectedValue = expectedValue;
    }

    @Override
    protected boolean matchesSafely(Map.Entry<String, String> item) {
      return expectedKey.equals(item.getKey()) && expectedValue.equals(item.getValue());
    }

    @Override
    public void describeTo(Description description) {
      description
          .appendText("entry with key ")
          .appendValue(expectedKey)
          .appendText(" and value ")
          .appendValue(expectedValue);
    }

    @Override
    protected void describeMismatchSafely(Map.Entry<String, String> item, Description mismatchDescription) {
      mismatchDescription
          .appendText("is an entry with key ")
          .appendValue(item.getKey())
          .appendText(" and value ")
          .appendValue(item.getValue());
    }

  }

}
