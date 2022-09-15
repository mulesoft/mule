/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.extension.api.client.OperationParameters;

import java.util.Map;
import java.util.Optional;

/**
 * Decorates a {@link OperationParameters} instance so that it carries a context {@link Event}
 *
 * @since 4.5.0
 */
public class EventedOperationsParameterDecorator implements OperationParameters {

  private final OperationParameters delegate;
  private final Event contextEvent;

  public EventedOperationsParameterDecorator(OperationParameters delegate, Event contextEvent) {
    this.delegate = delegate;
    this.contextEvent = contextEvent;
  }

  public Event getContextEvent() {
    return contextEvent;
  }

  @Override
  public Optional<String> getConfigName() {
    return delegate.getConfigName();
  }

  @Override
  public Map<String, Object> get() {
    return delegate.get();
  }
}
