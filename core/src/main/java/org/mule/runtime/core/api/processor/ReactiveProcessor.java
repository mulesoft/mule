/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.processor;

import org.mule.runtime.core.api.Event;

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
   * need to know the type of work the message processor will be performing and if it is blocking, cpu instensive or neither.
   *
   * @return the processing type for this processor.
   */
  ProcessingType getProccesingType();

  /**
   * Defines the type of processing that the processor will be doing.
   */
  enum ProcessingType {

    /**
     * CPU intensive processing such as calculation of transformation.
     */
    CPU,
    /**
     * Processing which neither blocks or is CPU instensive such as message passing, filtering and routing etc.
     */
    CPU_LITE,
    /**
     * Blocking processing such as blocking IO or anything that performs Thread.sleep()
     */
    BLOCKING
  }

}
