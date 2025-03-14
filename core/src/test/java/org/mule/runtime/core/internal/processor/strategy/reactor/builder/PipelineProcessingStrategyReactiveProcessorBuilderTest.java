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
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import reactor.test.publisher.TestPublisher;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PipelineProcessingStrategyReactiveProcessorBuilderTest {

  private PipelineProcessingStrategyReactiveProcessorBuilder builder;

  @Mock
  private ReactiveProcessor pipeline;

  @BeforeEach
  void setUp() {
    when(pipeline.apply(any())).thenAnswer(inv -> inv.getArgument(0));

    builder = PipelineProcessingStrategyReactiveProcessorBuilder
        .pipelineProcessingStrategyReactiveProcessorFrom(pipeline, ClassLoader.getPlatformClassLoader(), "id", "app");
  }

  @Test
  void pipelineProcessingStrategyReactiveProcessorFrom() {
    Arrays.stream(PipelineProcessingStrategyReactiveProcessorBuilder.class.getDeclaredMethods())
        .filter(m -> m.getName().startsWith("with"))
        .filter(m -> m.canAccess(builder))
        .forEach(m -> {
          try {
            Object[] values = ParameterMockingTestUtil.getParameterValues(m);
            m.invoke(builder, values);
          } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
          }
        });

    final ReactiveProcessor result = builder.build();
    result.apply(TestPublisher.create());

    assertThat(result, is(notNullValue()));
  }
}
