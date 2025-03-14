/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import org.mockito.Mockito;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParameterMockingTestUtil {

  static Object getValue(Parameter parameter) {
    if (parameter.getType().isAssignableFrom(Optional.class)) {
      Class<?> parameterizedType =
          (Class<?>) ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments()[0];
      return Optional.of(Mockito.mock(parameterizedType));
    } else if (ReactiveProcessor.class.isAssignableFrom(parameter.getType())) {
      ReactiveProcessor value = (ReactiveProcessor) mock(parameter.getType());
      when(value.apply(any())).thenAnswer(inv -> inv.getArgument(0));
      return value;
    } else if (InternalProfilingService.class.isAssignableFrom(parameter.getType())) {
      InternalProfilingService value = (InternalProfilingService) mock(parameter.getType());
      lenient().when(value.enrichWithProfilingEventFlux(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(value.setCurrentExecutionContext((Mono<?>) any(), any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(value.setCurrentExecutionContext((Flux<?>) any(), any())).thenAnswer(inv -> inv.getArgument(0));
      return value;
    } else if (parameter.getType().equals(Integer.TYPE)) {
      return 1;
    } else {
      return Mockito.mock(parameter.getType());
    }
  }

  static Object[] getParameterValues(Method m) {
    return Arrays.stream(m.getParameters()).map(ParameterMockingTestUtil::getValue).toArray();
  }
}
