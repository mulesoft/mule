/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.event;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * Provides capability to only accept handling certain MuleEvents.
 */
@NoImplement
public interface Acceptor {

  /**
   * @param event {@link CoreEvent} to route through exception handler
   * @return true if this {@link org.mule.runtime.core.api.exception.FlowExceptionHandler} should handler exception false
   *         otherwise
   */
  boolean accept(CoreEvent event);

  /**
   * @return true if accepts any message, false otherwise.
   */
  boolean acceptsAll();
}
