/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.processor.HasLocation;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;

import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

@ExtendWith(MockitoExtension.class)
class ProfilingUtilsTest {

  @Mock
  private MuleContext context;
  @Mock
  private MuleConfiguration configuration;
  @Mock
  private ProfilingEventType<? extends ProfilingEventContext> eventType;

  @ParameterizedTest(name = "{0}")
  @MethodSource("locationTestData")
  void getLocation(String name, ReactiveProcessor processor, Consumer<ReactiveProcessor> verifier) {

    final ComponentLocation result = ProfilingUtils.getLocation(processor);

    // Normally it's not null, but it's all mocking - this is just here to make SonarQube happy because it's not clever enough
    // to figure out that the verifier actually does verify stuff. :face_with_rolling_eyes:
    assertThat(result, is(nullValue()));
    verifier.accept(processor);
  }

  static Stream<Arguments> locationTestData() {
    return Stream.of(
                     args("HasLocation", getProcessor(HasLocation.class),
                          o -> verify((HasLocation) o).resolveLocation()),
                     args("InterceptedReactiveProcessor",
                          new InterceptedReactiveProcessor(getProcessor(HasLocation.class),
                                                           mock(ReactiveProcessor.class)),
                          o -> verify((HasLocation) ((InterceptedReactiveProcessor) o).getProcessor()).resolveLocation()),
                     args("Component", getProcessor(Component.class),
                          o -> verify((Component) o).getLocation()),
                     args("Plain", mock(ReactiveProcessor.class), Mockito::verifyNoMoreInteractions));
  }

  private static ReactiveProcessor getProcessor(Class<?> extraInterface) {
    return mock(ReactiveProcessor.class, withSettings().extraInterfaces(extraInterface));
  }

  private static Arguments args(String name, ReactiveProcessor processor, Consumer<ReactiveProcessor> verifier) {
    return Arguments.of(name, processor, verifier);
  }

  @Test
  void getArtifactId() {
    when(context.getConfiguration()).thenReturn(configuration);
    when(configuration.getId()).thenReturn("foo");

    final String result = ProfilingUtils.getArtifactId(context);

    assertThat(result, is("foo"));
    verify(configuration).getId();
  }

  @Test
  void getArtifactType() {
    when(context.getArtifactType()).thenReturn(ArtifactType.APP);

    final String result = ProfilingUtils.getArtifactType(context);

    assertThat(result, is("app"));
  }

  @Test
  void getFullyQualifiedProfilingEventTypeIdentifier() {
    when(eventType.getProfilingEventTypeIdentifier()).thenReturn("bar");
    when(eventType.getProfilingEventTypeNamespace()).thenReturn("foo");

    final String result = ProfilingUtils.getFullyQualifiedProfilingEventTypeIdentifier(eventType);

    assertThat(result, is("foo:bar"));
  }
}
