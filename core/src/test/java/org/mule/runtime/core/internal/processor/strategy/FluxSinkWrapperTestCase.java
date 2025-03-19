/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.processor.strategy.reactor.builder.ParameterMockingTestUtil;
import reactor.core.publisher.FluxSink;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.verify;

/**
 * Coverage tests for {@link FluxSinkWrapper} Mostly the tests just make sure that the delegate is called when the wrapper method
 * is called.
 */
@ExtendWith(MockitoExtension.class)
class FluxSinkWrapperTestCase {

  private FluxSinkWrapper sink;
  @Mock
  private FluxSink<CoreEvent> delegate;

  @BeforeEach
  void setUp() {
    sink = new FluxSinkWrapper(delegate);
  }

  @ParameterizedTest(name = "[{index}]{0}")
  @MethodSource("fluxSinkMethods")
  void fluxSinkDelegates(String testName, Method m) throws InvocationTargetException, IllegalAccessException {
    Object[] values = ParameterMockingTestUtil.getParameterValues(m, CoreEvent.class);
    m.invoke(sink, values);
    // Is this sneaky? Calling 'verify' puts the object into verification mode, so when the method is invoked on the mock it
    // verifies the activity
    m.invoke(verify(delegate), values);
  }

  static List<Arguments> fluxSinkMethods() {
    return Arrays.stream(FluxSink.class.getDeclaredMethods())
        .filter(m -> m.getName().matches("^[a-z].*"))
        // Filter out contextView - it's a default inherited from the interface
        .filter(m -> !m.getName().equals("contextView"))
        .map(m -> Arguments.of(m.getName(), m))
        .toList();
  }

  /**
   * contextView is not overridden in FluxSinkWrapper, so it's the default implementation on the interface we're testing...
   */
  @Test
  void contextView() {
    sink.contextView();
    verify(delegate).currentContext();
  }
}
