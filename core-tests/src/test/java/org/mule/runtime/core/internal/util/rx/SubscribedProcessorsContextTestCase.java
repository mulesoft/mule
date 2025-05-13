/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import static org.mule.runtime.core.internal.util.rx.SubscribedProcessorsContext.subscribedProcessors;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.EXECUTION_ENGINE;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.REACTOR;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;
import static reactor.core.publisher.Flux.create;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

@Feature(EXECUTION_ENGINE)
@Story(REACTOR)
@Issue("W-13994259")
public class SubscribedProcessorsContextTestCase extends AbstractMuleTestCase {

  @Test
  public void contextMustIncorporateDownstreamModification() {
    List<Throwable> subscriptionErrors = new ArrayList<>();
    create(FluxSink::complete)
        .transformDeferredContextual((objectFlux,
                                      contextView) -> objectFlux
                                          .doOnSubscribe(subscription -> assertThat(contextView.get("TEST_KEY"),
                                                                                    is("UPDATED_TEST_VALUE"))))
        .contextWrite(context -> subscribedProcessors(context)
            .addSubscribedProcessor(mock(Processor.class)))
        .contextWrite(context -> context.put("TEST_KEY", "UPDATED_TEST_VALUE"))
        .contextWrite(context -> subscribedProcessors(context)
            .addSubscribedProcessor(mock(Processor.class)))
        .contextWrite(context -> context.put("TEST_KEY", "INITIAL_TEST_VALUE"))
        .subscribe(next -> {
        }, subscriptionErrors::add);
    assertThat("Subscription errors where found.", subscriptionErrors, is(empty()));
  }

  @Test
  public void contextMustCountSubscribedProcessors() {
    List<Throwable> subscriptionErrors = new ArrayList<>();
    create(FluxSink::complete)
        .transformDeferredContextual((objectFlux,
                                      contextView) -> objectFlux
                                          .doOnSubscribe(subscription -> assertThat("Wrong subscribed processors count.",
                                                                                    subscribedProcessors(contextView).get()
                                                                                        .getSubscribedProcessorsCount(),
                                                                                    is(2))))
        .contextWrite(context -> subscribedProcessors(context)
            .addSubscribedProcessor(mock(Processor.class)))
        .contextWrite(context -> subscribedProcessors(context)
            .addSubscribedProcessor(mock(Processor.class)))
        .subscribe(next -> {
        }, subscriptionErrors::add);
    assertThat("Subscription errors where found.", subscriptionErrors, is(empty()));
  }

  @Test
  public void contextMustAccumulateSubscribedProcessorsWhenConfigured() {
    ComponentLocation location = mock(ComponentLocation.class);
    when(location.getLocation()).thenReturn("processorLocation");
    Processor processor = mock(Processor.class, withSettings().extraInterfaces(Component.class));
    when(((Component) processor).getLocation()).thenReturn(location);

    ComponentLocation anotherLocation = mock(ComponentLocation.class);
    when(anotherLocation.getLocation()).thenReturn("anotherProcessorLocation");
    Processor anotherProcessor = mock(Processor.class, withSettings().extraInterfaces(Component.class));
    when(((Component) anotherProcessor).getLocation()).thenReturn(anotherLocation);

    List<Throwable> subscriptionErrors = new ArrayList<>();
    create(FluxSink::complete)
        .transformDeferredContextual((objectFlux,
                                      contextView) -> objectFlux
                                          .doOnSubscribe(subscription -> assertThat("Wrong subscribed components count.",
                                                                                    subscribedProcessors(contextView).get()
                                                                                        .getSubscribedComponents().size(),
                                                                                    is(2))))
        .contextWrite(context -> subscribedProcessors(context)
            .addSubscribedProcessor(anotherProcessor))
        .contextWrite(context -> subscribedProcessors(context, true)
            .addSubscribedProcessor(processor))
        .subscribe(next -> {
        }, subscriptionErrors::add);
    assertThat("Subscription errors where found.", subscriptionErrors, is(empty()));
  }

  @Test
  public void contextMustBeImmutable() {
    List<SubscribedProcessorsContext> contexts = new ArrayList<>();
    create(FluxSink::complete)
        .contextWrite(context -> {
          Context updatedContext = subscribedProcessors(context)
              .addSubscribedProcessor(mock(Processor.class));
          contexts.add(subscribedProcessors(updatedContext));
          return updatedContext;
        })
        .contextWrite(context -> {
          Context updatedContext = subscribedProcessors(context)
              .addSubscribedProcessor(mock(Processor.class));
          contexts.add(subscribedProcessors(context));
          contexts.add(subscribedProcessors(updatedContext));
          return updatedContext;
        })
        .subscribe();
    assertThat(contexts.size(), is(3));
    assertThat(contexts.get(0).getSubscribedProcessorsCount(), is(0));
    assertThat(contexts.get(1).getSubscribedProcessorsCount(), is(1));
    assertThat(contexts.get(2).getSubscribedProcessorsCount(), is(2));
  }

}
