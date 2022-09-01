/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.adapter;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.extension.api.client.OperationParameters;
import org.mule.runtime.extension.internal.client.InternalOperationParameters;

import java.util.Map;
import java.util.Optional;

/**
 * Adapts a {@link org.mule.sdk.api.client.OperationParameters} into a legacy {@link OperationParameters}
 *
 * @since 4.5.0
 */
public class LegacyOperationParametersAdapter implements InternalOperationParameters {

  private final org.mule.sdk.api.client.OperationParameters delegate;
  private Event contextEvent;

  public LegacyOperationParametersAdapter(org.mule.sdk.api.client.OperationParameters delegate) {
    this.delegate = delegate;
  }

  @Override
  public Optional<String> getConfigName() {
    return delegate.getConfigName();
  }

  @Override
  public Map<String, Object> get() {
    return delegate.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Event> getContextEvent() {
    return ofNullable(contextEvent);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setContextEvent(Event contextEvent) {
    this.contextEvent = contextEvent;
  }
}
