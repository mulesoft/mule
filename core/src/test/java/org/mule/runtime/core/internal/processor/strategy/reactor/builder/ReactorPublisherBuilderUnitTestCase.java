/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.reactor.builder;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.publisher.TestPublisher;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ReactorPublisherBuilderUnitTestCase {

  private static List<Method> methods;

  @Mock
  private CoreEvent event;

  @BeforeAll
  static void setup() {
    methods = Arrays.stream(ReactorPublisherBuilder.class.getDeclaredMethods())
        .filter(m -> m.getName().matches("^[a-z].*"))
        .toList();
  }

  @Test
  void buildMono() {
    final ReactorPublisherBuilder<Mono<CoreEvent>> result = ReactorPublisherBuilder.buildMono(event);
    setValues(result);
    assertThat(result.build(), is(instanceOf(Mono.class)));
  }

  @Test
  void buildFlux() {
    final ReactorPublisherBuilder<Flux<CoreEvent>> result = ReactorPublisherBuilder.buildFlux(TestPublisher.create());
    setValues(result);
    assertThat(result.build(), is(instanceOf(Flux.class)));
  }

  private static void setValues(ReactorPublisherBuilder<?> result) {
    methods.forEach(m -> {
      try {
        Object[] values = ParameterMockingTestUtil.getParameterValues(m);
        m.invoke(result, values);
      } catch (IllegalAccessException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    });
  }

}
