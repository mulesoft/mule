/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.util.rx;

import org.mule.runtime.api.lifecycle.Disposable;

import java.util.function.Supplier;

import reactor.core.publisher.FluxSink;
import reactor.util.context.ContextView;

/**
 * Supplier of {@link FluxSink}.
 *
 * @param <T> the value type
 *
 * @since 4.3
 */
public interface FluxSinkSupplier<T> extends Supplier<FluxSink<T>>, Disposable {

  /**
   * Get sink taking into account the given {@link ContextView}. This can be used to know if there is an active transaction.
   *
   * @since 4.5, 4.4.1, 4.3.1
   */
  default FluxSink<T> get(ContextView ctx) {
    return this.get();
  }
}
