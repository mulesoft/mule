/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.http.internal;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.service.http.api.domain.ParameterMap;

import org.junit.Test;

public class CaseInsensitiveParameterMapTestCase extends ParameterMapTestCase {

  @Override
  protected ParameterMap getParameterMap() {
    return new CaseInsensitiveParameterMap(new ParameterMap());
  }

  @Test
  public void takesParamMapEntries() {
    ParameterMap sensitiveParameterMap = new ParameterMap();
    sensitiveParameterMap.put(KEY_1, VALUE_1);
    sensitiveParameterMap.put(KEY_2, VALUE_1);
    sensitiveParameterMap.put(KEY_2, VALUE_2);
    CaseInsensitiveParameterMap insensitiveParameterMap = new CaseInsensitiveParameterMap(sensitiveParameterMap);

    assertThat(insensitiveParameterMap.get(KEY_1), is(VALUE_1));
    assertThat(insensitiveParameterMap.get(KEY_1.toLowerCase()), is(VALUE_1));
    assertThat(insensitiveParameterMap.get(KEY_2), is(VALUE_2));
    assertThat(insensitiveParameterMap.get(KEY_2.toLowerCase()), is(VALUE_2));

    assertThat(insensitiveParameterMap.getAll(KEY_1), is(asList(VALUE_1)));
    assertThat(insensitiveParameterMap.getAll(KEY_1.toLowerCase()), is(asList(VALUE_1)));
    assertThat(insensitiveParameterMap.getAll(KEY_2), is(asList(VALUE_1, VALUE_2)));
    assertThat(insensitiveParameterMap.getAll(KEY_2.toLowerCase()), is(asList(VALUE_1, VALUE_2)));
  }

  @Test
  public void putAndGetCase() {
    assertThat(parameterMap.put("kEy", VALUE_1), nullValue());
    assertThat(parameterMap.get("KeY"), is(VALUE_1));
    assertThat(parameterMap.get("kEy"), is(VALUE_1));
    assertThat(parameterMap.getAll("key"), is(asList(VALUE_1)));
    assertThat(parameterMap.getAll("KEY"), is(asList(VALUE_1)));
  }

  @Test
  public void aggregatesSameCaseKeys() {
    assertThat(parameterMap.put("kEy", VALUE_1), nullValue());
    assertThat(parameterMap.put("KeY", VALUE_2), is(VALUE_1));
    assertThat(parameterMap.get("key"), is(VALUE_2));
    assertThat(parameterMap.getAll("KEY"), is(asList(VALUE_1, VALUE_2)));
  }

}
