/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;

import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Processes streams of {@link Event}'s using a functional approach based on <a href="http://www.reactive-streams.org/">Reactive
 * Streams<a/> where {@link Event} processing is defined via stream functions.
 *
 * @since 4.0
 */
public interface ReactiveProcessor extends Function<Publisher<Event>, Publisher<Event>> {

  /**
   * In order for Mule to determine the best way to execute different processors based on the chosen {@link ProcessingStrategy} it
   * needs to know the type of work the message processor will be performing and if it is {@link ProcessingType#BLOCKING},
   * {@link ProcessingType#CPU} intensive or neither ({@link ProcessingType#CPU_LITE}).
   *
   * @return the processing type for this processor.
   */
  ProcessingType getProccesingType();

  /**
   * Defines the type of processing that the processor will be doing.
   */
  enum ProcessingType {

    /**
     * CPU intensive processing such as calculation or transformation.
     */
    CPU,
    /**
     * Processing which neither blocks nor is CPU intensive such as message passing, filtering and routing etc.
     */
    CPU_LITE,
    /**
     * Blocking processing such as blocking IO or anything that may block the current thread.
     */
    BLOCKING
  }

}
