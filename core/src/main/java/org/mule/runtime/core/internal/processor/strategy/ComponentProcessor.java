/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.processor.ReactiveProcessor;

/**
 * Specialization of {@link ReactiveProcessor} that allows to perform certain optimizations when the processor is not blocking, as
 * indicated by its {@link #isBlocking()} method and that evaluates if a thread switch may happen, as indicated by its
 * {@link #canBeAsync()}
 * </p>
 * <b>IMPORTANT!</b> The processing strategy will delegate the parallel processing of events to the implementation, so it is
 * required that implementations properly handle parallel processing.
 *
 * @since 4.3
 */
public interface ComponentProcessor extends ReactiveProcessor {

  /**
   * An blocking processor is one that will perform blocking calls. So it is not guaranteed that a thread switch happens or that
   * the operation returns without blocking.
   *
   * @return {@code} if the processor is blocking.
   */
  boolean isBlocking();

  /**
   * An async processor is one that may change the thread where it is executing but not always. For example if a reconnection
   * strategy is applied and a connection problem happens
   *
   * @return {@code} if the processor may be asynchronous.
   */
  boolean canBeAsync();
}
