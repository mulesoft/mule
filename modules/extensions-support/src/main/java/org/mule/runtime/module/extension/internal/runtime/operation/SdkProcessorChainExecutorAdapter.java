/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.internal.util.message.SdkResultAdapter.from;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.processor.chain.HasMessageProcessors;
import org.mule.runtime.module.extension.internal.runtime.source.legacy.LegacyResultAdapter;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.route.Chain;
import org.mule.sdk.api.runtime.route.Route;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Adapts a {@link ProcessorChainExecutor} for {@link Chain} and {@link Route} implementations written with the new sdk-api
 *
 * @since 4.8.0
 */
public class SdkProcessorChainExecutorAdapter implements Chain, HasMessageProcessors, InputEventAware {

  private static Consumer<org.mule.runtime.extension.api.runtime.operation.Result> adapt(Consumer<Result> onSuccess) {
    return r -> onSuccess.accept(from(r));
  }

  private static BiConsumer<Throwable, org.mule.runtime.extension.api.runtime.operation.Result> adapt(BiConsumer<Throwable, Result> onError) {
    return (t, r) -> onError.accept(t, from(r));
  }

  private final ProcessorChainExecutor delegate;

  public SdkProcessorChainExecutorAdapter(ProcessorChainExecutor delegate) {
    this.delegate = delegate;
  }

  @Override
  public void process(Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    delegate.process(adapt(onSuccess), adapt(onError));
  }

  @Override
  public void process(Object payload, Object attributes, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    delegate.process(payload, attributes, adapt(onSuccess), adapt(onError));
  }

  @Override
  public void process(Result input, Consumer<Result> onSuccess, BiConsumer<Throwable, Result> onError) {
    delegate.process(LegacyResultAdapter.from(input), adapt(onSuccess), adapt(onError));
  }

  @Override
  public CoreEvent getOriginalEvent() {
    return delegate.getOriginalEvent();
  }

  @Override
  public List<Processor> getMessageProcessors() {
    return delegate.getMessageProcessors();
  }
}
