/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.matcher;

import static org.junit.Assert.assertThat;

import org.mule.service.http.api.domain.ParameterMap;

import java.util.List;
import java.util.Map;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.core.Is;

public class ParamMapMatcher {

  public static Matcher<ParameterMap> isEqual(final Map<String, ? extends List<String>> parameters) {
    return new BaseMatcher<ParameterMap>() {

      @Override
      public boolean matches(Object o) {
        ParameterMap parameterMap = (ParameterMap) o;
        assertThat(parameterMap.size(), Is.is(parameters.size()));
        for (String key : parameters.keySet()) {
          assertThat(parameterMap.keySet(),
                     Matchers.containsInAnyOrder(parameters.keySet().toArray(new String[parameterMap.size()])));
          final List<String> parameterKeyValues = parameters.get(key);
          final List<String> parameterMapValues = parameterMap.getAll(key);
          assertThat(parameterMapValues,
                     Matchers.containsInAnyOrder(parameterKeyValues.toArray(new String[parameterKeyValues.size()])));
        }
        return true;
      }

      @Override
      public void describeTo(Description description) {}
    };
  }

}
