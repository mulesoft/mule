/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.strategy.reactor.builder.ParameterMockingTestUtil;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;

class SimpleRetryPolicyTemplateTestCase {

  private SimpleRetryPolicyTemplate template;

  @BeforeEach
  void setUp() {
    template = new SimpleRetryPolicyTemplate(200L, 10);
  }

  @Test
  void createRetryInstance() {
    final RetryPolicy result = template.createRetryInstance();
    assertThat(result, is(notNullValue()));
  }

  @Test
  void testToString() {
    final String result = template.toString();
    assertThat(result, containsString("frequency=200"));
    assertThat(result, containsString("retryCount=10"));
  }

  @ParameterizedTest(name = "[{index}]{0}")
  @MethodSource("fluxSinkMethods")
  void fluxSinkDelegates(String testName, Method getter, Method setter) throws InvocationTargetException, IllegalAccessException {
    Object[] values = ParameterMockingTestUtil.getParameterValues(setter, CoreEvent.class);
    setter.invoke(template, values);
    assertThat(getter.invoke(template), is(values[0]));
  }

  static List<Arguments> fluxSinkMethods() {
    return Arrays.stream(SimpleRetryPolicyTemplate.class.getDeclaredMethods())
        .filter(m -> m.getName().matches("^(get|set).*"))
        .collect(Collectors.groupingBy(m -> m.getName().substring(3))).values().stream()
        .filter(methods -> methods.size() == 2)
        .map(methods -> methods.stream().sorted(Comparator.comparing(Method::getName)).toList())
        .map(methods -> Arguments.of(methods.get(0).getName(), methods.get(0), methods.get(1)))
        .toList();
  }
}
