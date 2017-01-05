/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.execution.interception;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.interception.ProcessorInterceptorCallback;
import org.mule.runtime.dsl.api.component.ComponentIdentifier;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link ProcessorInterceptorCallback} that logs each before and after operations.
 * It does not intercept the process operation.
 *
 * @since 4.0
 */
public class LoggerProcessorInterceptorCallback implements ProcessorInterceptorCallback {

  private transient Logger logger = LoggerFactory.getLogger(LoggerProcessorInterceptorCallback.class);

  private ComponentIdentifier componentIdentifier;

  /**
   * Creates an callback for the given componentIdentifier and parameters.
   * @param componentIdentifier {@link ComponentIdentifier} for the intercepted processor.
   */
  public LoggerProcessorInterceptorCallback(ComponentIdentifier componentIdentifier) {
    this.componentIdentifier = componentIdentifier;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void before(Event event, Map<String, Object> parameters) {
    if (logger.isTraceEnabled()) {
      logger.trace("BEFORE: '{}', parameters: {}", componentIdentifier, parameters);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean shouldInterceptExecution(Event event, Map<String, Object> parameters) {
    return false;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Event getResult(Event event) throws MuleException {
    return event;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void after(Event resultEvent, Map<String, Object> parameters) {
    if (logger.isTraceEnabled()) {
      logger.trace("AFTER: '{}', parameters: {}", componentIdentifier, parameters);
    }
  }

}
