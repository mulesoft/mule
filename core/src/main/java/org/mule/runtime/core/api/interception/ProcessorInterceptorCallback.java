/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.api.interception;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;

import java.util.Map;

/**
 * When a {@link org.mule.runtime.core.api.processor.Processor} is intercepted an instance of this class
 * will be used to handle the interception.
 * <p/>
 * {@link #shouldInterceptExecution(Event, Map<String, Object>)} would be called before actually processing the event by the intercepted
 * {@link org.mule.runtime.core.api.processor.Processor}, if this returns {@code true} the intercepted processor
 * would be called, in case of {@code false} the {@link #getResult(Event)} should be called instead.
 * <p/>
 * The {@link #getResult(Event)} method allows to generate generate a different {@link Event}.
 *
 * @since 4.0
 */
public interface ProcessorInterceptorCallback {

  /**
   * This method is invoked before the event is processed.
   *
   * @param event      {@link Event} to be processed.
   * @param parameters {@link Map<String, Object>} with the parameters resolved for the processor component definition.
   */
  default void before(Event event, Map<String, Object> parameters) {}

  /**
   * Decides if the intercepted {@link org.mule.runtime.core.api.processor.Processor} should be invoked for processing
   * the event or {@link #getResult(Event)} instead.
   *
   * @param event      {@link Event} to be processed.
   * @param parameters {@link Map<String, Object>} with the parameters resolved for the processor component definition.
   * @return {@code false} if the intercepted {@link org.mule.runtime.core.api.processor.Processor} should be invoked or {@code true}
   * if this interceptor callback should generate the processed event.
   */
  default boolean shouldInterceptExecution(Event event, Map<String, Object> parameters) {
    return false;
  }

  /**
   * This method is called in order to replace the process logic of the intercepted {@link org.mule.runtime.core.api.processor.Processor}
   * when {@link #shouldInterceptExecution(Event, Map<String, Object>)} returns {@code true}. The returned {@link Event} here would be the one passed
   * to the next processor in chain.
   *
   * @param event {@link Event} to be processed.
   * @return {@link Event} with the process result.
   * @throws MuleException if there was a problem while processing the {@link Event}.
   */
  Event getResult(Event event) throws MuleException;

  /**
   * This method is invoked after the event has been processed, unless an exception was thrown.
   *
   * @param resultEvent {@link Event} processed.
   * @param parameters {@link Map<String, Object>} with the parameters resolved for the processor component definition.
   */
  default void after(Event resultEvent, Map<String, Object> parameters) {}

}
