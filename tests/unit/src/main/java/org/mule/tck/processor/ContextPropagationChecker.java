/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.tck.processor;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Flux.just;
import static reactor.core.publisher.Mono.subscriberContext;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;

import java.util.function.Function;

import org.reactivestreams.Publisher;

import reactor.util.context.Context;

/**
 * Processor to use in unit test cases in order to assert that subscription context is properly propagated.
 *
 * @since 4.3
 */
public class ContextPropagationChecker implements Processor {

  private static final String CTX_PROPAGATED_KEY = "ctxPropagated";

  @Override
  public CoreEvent process(CoreEvent event) throws MuleException {
    fail("Need `apply` to be called instead of `process`.");
    return event;
  }

  @Override
  public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
    return subscriberContext()
        .flatMapMany(ctx -> from(publisher)
            .doOnNext(e -> assertThat(ctx.getOrEmpty(CTX_PROPAGATED_KEY).orElse(false), is(true))));
  }

  public Function<Context, Context> contextPropagationFlag() {
    return ctx -> ctx.put(CTX_PROPAGATED_KEY, true);
  }

  /**
   *
   * @param event the event to test with
   * @param routerOrScope the router or scope containing {@code checker} to validate.
   * @param checker the processor that validates the context
   */
  public static final void assertContextPropagation(CoreEvent event, Processor routerOrScope, ContextPropagationChecker checker) {
    final CoreEvent result = just(event)
        .transform(routerOrScope)
        .subscriberContext(checker.contextPropagationFlag())
        .blockFirst();

    assertThat(result, not(nullValue()));
  }
}
