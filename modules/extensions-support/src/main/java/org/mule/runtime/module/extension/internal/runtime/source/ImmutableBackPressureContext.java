/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.extension.api.runtime.source.BackPressureAction;
import org.mule.runtime.extension.api.runtime.source.BackPressureContext;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;

/**
 * Immutable implementation of {@link BackPressureContext}
 *
 * @since 1.1
 */
public final class ImmutableBackPressureContext implements BackPressureContext {

  private final Event event;
  private final BackPressureAction action;
  private final SourceCallbackContext sourceCallbackContext;

  public ImmutableBackPressureContext(Event event,
                                      BackPressureAction action,
                                      SourceCallbackContext sourceCallbackContext) {
    this.event = event;
    this.action = action;
    this.sourceCallbackContext = sourceCallbackContext;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Event getEvent() {
    return event;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public BackPressureAction getAction() {
    return action;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SourceCallbackContext getSourceCallbackContext() {
    return sourceCallbackContext;
  }
}
