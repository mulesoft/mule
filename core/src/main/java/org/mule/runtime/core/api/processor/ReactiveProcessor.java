/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
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
   * {@link ProcessingType#CPU_INTENSIVE} intensive or neither ({@link ProcessingType#CPU_LITE}).
   *
   * @return the processing type for this processor.
   */
  default ProcessingType getProccesingType() {
    return CPU_LITE;
  }

  /**
   * Defines the type of processing that the processor will be doing.
   */
  enum ProcessingType {

    /**
     * CPU intensive processing such as calculation or transformation.
     */
    CPU_INTENSIVE,
    /**
     * Processing which neither blocks nor is CPU intensive such as message passing, filtering, routing or non-blocking IO..
     */
    CPU_LITE,
    /**
     * Blocking processing that use Thead.sleep or any other technique to block the current thread during processing.
     */
    BLOCKING,
    /**
     * Blocking IO read/write operations. This is treated seperatly to {@link #BLOCKING} to allow for potential optimizations when
     * IO is fast and/or message sizes smalls.
     */
    IO_RW

  }

}
