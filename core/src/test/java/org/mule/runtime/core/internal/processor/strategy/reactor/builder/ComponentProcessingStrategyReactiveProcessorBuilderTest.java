/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.processor.ReactiveProcessor;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ComponentProcessingStrategyReactiveProcessorBuilderTest {

  private ComponentProcessingStrategyReactiveProcessorBuilder builder;
  @Mock
  private Scheduler scheduler;
  @Mock
  private ReactiveProcessor processor;

  @BeforeEach
  void setUp() {
    builder = new ComponentProcessingStrategyReactiveProcessorBuilder(processor, scheduler, "appId", "app");
  }

  @Test
  void build() {
    Arrays.stream(ComponentProcessingStrategyReactiveProcessorBuilder.class.getDeclaredMethods())
        .filter(m -> m.getName().startsWith("with"))
        .filter(m -> m.canAccess(builder))
        .forEach(m -> {
          try {
            final Object value = mock(m.getParameters()[0].getClass());
            m.invoke(builder, value);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        });

    final ReactiveProcessor result = builder.build();

    assertThat(result, is(notNullValue()));
  }
}
