/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.event.PrivilegedEvent;

/**
 * Helper class that creates an {@link CoreEvent} maintaining variables from different scopes.
 *
 * @since 4.0
 */
public class PolicyEventConverter {

  /**
   * Creates a new {@link CoreEvent} based on a message and another event which is used to get the variables and
   * {@link BaseEventContext}
   *
   * @param event provider of the message and session parts of the event
   * @param variablesProviderEvent provider of the variables part of the event
   * @return the created event
   */
  public PrivilegedEvent createEvent(PrivilegedEvent event, PrivilegedEvent variablesProviderEvent) {
    return PrivilegedEvent.builder(event).variables(variablesProviderEvent.getVariables()).build();
  }

}
