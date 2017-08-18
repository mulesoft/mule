/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.http.api.domain;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_SERVICE;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.MULTI_MAP;
import org.mule.runtime.api.util.MultiMap;
import org.mule.runtime.api.util.MultiMapTestCase;

import org.junit.Test;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(HTTP_SERVICE)
@Story(MULTI_MAP)
public class CaseInsensitiveMultiMapTestCase extends MultiMapTestCase {

  @Override
  protected MultiMap<String, String> getMultiMap() {
    return new CaseInsensitiveMultiMap(new MultiMap<>());
  }

  @Test
  public void takesParamMapEntries() {
    MultiMap<String, String> sensitiveMultiMap = new MultiMap<>();
    sensitiveMultiMap.put(KEY_1, VALUE_1);
    sensitiveMultiMap.put(KEY_2, VALUE_1);
    sensitiveMultiMap.put(KEY_2, VALUE_2);
    CaseInsensitiveMultiMap insensitiveMultiMap = new CaseInsensitiveMultiMap(sensitiveMultiMap);

    assertThat(insensitiveMultiMap.get(KEY_1), is(VALUE_1));
    assertThat(insensitiveMultiMap.get(KEY_1.toLowerCase()), is(VALUE_1));
    assertThat(insensitiveMultiMap.get(KEY_2), is(VALUE_1));
    assertThat(insensitiveMultiMap.get(KEY_2.toLowerCase()), is(VALUE_1));

    assertThat(insensitiveMultiMap.getAll(KEY_1), is(asList(VALUE_1)));
    assertThat(insensitiveMultiMap.getAll(KEY_1.toLowerCase()), is(asList(VALUE_1)));
    assertThat(insensitiveMultiMap.getAll(KEY_2), is(asList(VALUE_1, VALUE_2)));
    assertThat(insensitiveMultiMap.getAll(KEY_2.toLowerCase()), is(asList(VALUE_1, VALUE_2)));
  }

  @Test
  public void putAndGetCase() {
    assertThat(multiMap.put("kEy", VALUE_1), nullValue());
    assertThat(multiMap.get("KeY"), is(VALUE_1));
    assertThat(multiMap.get("kEy"), is(VALUE_1));
    assertThat(multiMap.getAll("key"), is(asList(VALUE_1)));
    assertThat(multiMap.getAll("KEY"), is(asList(VALUE_1)));
  }

  @Test
  public void aggregatesSameCaseKeys() {
    assertThat(multiMap.put("kEy", VALUE_1), nullValue());
    assertThat(multiMap.put("KeY", VALUE_2), is(VALUE_1));
    assertThat(multiMap.get("key"), is(VALUE_1));
    assertThat(multiMap.getAll("KEY"), is(asList(VALUE_1, VALUE_2)));
  }

}
