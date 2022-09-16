/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
