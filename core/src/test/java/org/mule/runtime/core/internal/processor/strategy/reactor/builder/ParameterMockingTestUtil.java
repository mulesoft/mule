/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import org.mockito.Mockito;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.profiling.InternalProfilingService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.sound.sampled.Control;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Collect some logic to create mock parameters for method invocations in tests. This is intended for tests that are exercising
 * simple methods (such as on builders) that don't need much behaviour, just the objects so the method can be invoked.
 *
 */
public class ParameterMockingTestUtil {

  static Object getValue(Parameter parameter, Class<?> typeClass) {
    if (Optional.class.isAssignableFrom(parameter.getType())) {
      Class<?> parameterizedType = parameter.getParameterizedType() instanceof TypeVariable ? typeClass
          : (Class<?>) ((ParameterizedType) parameter.getParameterizedType()).getActualTypeArguments()[0];
      return Optional.of(Mockito.mock(parameterizedType));
    } else if (parameter.getParameterizedType() instanceof TypeVariable<?>) {
      // the parameter is a generic type variable... so return the type we're using for generics.
      // If we wind up with multiples we'll have to be more clever
      return mock(typeClass);
    } else if (ReactiveProcessor.class.isAssignableFrom(parameter.getType())) {
      ReactiveProcessor value = (ReactiveProcessor) mock(parameter.getType());
      when(value.apply(any())).thenAnswer(inv -> inv.getArgument(0));
      return value;
    } else if (InternalProfilingService.class.isAssignableFrom(parameter.getType())) {
      InternalProfilingService value = (InternalProfilingService) mock(parameter.getType());
      lenient().when(value.enrichWithProfilingEventFlux(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(value.enrichWithProfilingEventMono(any(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(value.setCurrentExecutionContext((Mono<?>) any(), any())).thenAnswer(inv -> inv.getArgument(0));
      lenient().when(value.setCurrentExecutionContext((Flux<?>) any(), any())).thenAnswer(inv -> inv.getArgument(0));
      return value;
    } else if (parameter.getType().equals(Integer.TYPE)) {
      return 1;
    } else if (parameter.getType().equals(Long.TYPE)) {
      return 2L;
    } else {
      return Mockito.mock(parameter.getType());
    }
  }

  public static Object[] getParameterValues(Method m) {
    return getParameterValues(m, Object.class);
  }

  public static Object[] getParameterValues(Method m, final Class<?> typeClass) {
    return Arrays.stream(m.getParameters()).map(p -> getValue(p, typeClass)).toArray();
  }
}
