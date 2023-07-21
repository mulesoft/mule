/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.client.operation;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.util.Optional.ofNullable;

import org.mule.runtime.api.event.Event;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.client.OperationParameterizer;
import org.mule.runtime.module.extension.internal.runtime.client.params.BaseComponentParameterizer;

import java.util.Optional;

/**
 * Default implementation of {@link OperationParameterizer}
 *
 * @since 4.5.0
 */
public class DefaultOperationParameterizer extends BaseComponentParameterizer<OperationParameterizer>
    implements InternalOperationParameterizer {

  private CoreEvent contextEvent;

  @Override
  public OperationParameterizer inTheContextOf(Event event) {
    checkArgument(event instanceof CoreEvent, "event must be an instance of " + CoreEvent.class.getSimpleName());
    this.contextEvent = (CoreEvent) event;
    return this;
  }

  @Override
  public Optional<CoreEvent> getContextEvent() {
    return ofNullable(contextEvent);
  }
}
