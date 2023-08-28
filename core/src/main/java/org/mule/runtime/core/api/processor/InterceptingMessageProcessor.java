/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

/**
 * <p>
 * Processes {@link CoreEvent}'s intercepting another listener {@link Processor}. It is the InterceptingMessageProcessor's
 * responsibility to invoke the next {@link Processor}.
 * </p>
 * Although not normal, it is valid for the <i>listener</i> MessageProcessor to be <i>null</i> and implementations should handle
 * this case.
 *
 * @since 3.0
 *
 * @deprecated Use interception API instead of this interface. Ref: {@link ProcessorInterceptorFactory}.
 */
@Deprecated
public interface InterceptingMessageProcessor extends Processor {

  /**
   * Set the MessageProcessor listener that will be invoked when a message is received or generated.
   */
  void setListener(Processor listener);

  /**
   * Indicates if the processing strategy of the parent flow is applied to the intercepted processor's chain.
   *
   * @return {@code true} if the {@code listener} will run wholly in the same thread and blocking if needed. {@code false} if the
   *         lfow's {@link ProcessingStrategy} will be used.
   */
  default boolean isBlocking() {
    return false;
  }
}
