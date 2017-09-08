/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;

/**
 * Exception used to signal the failure exception of the flow.
 * <p>
 * This exception is to differentiate between the failure of the policy execution logic from the flow execution logic.
 *
 * @since 4.0
 */
public class FlowExecutionException extends MessagingException {

  /**
   * Creates a new {@link FlowExecutionException}.
   *
   * @param event the event result of the flow execution
   * @param cause the cause of the failure
   * @param failingComponent the component that failed
   */
  public FlowExecutionException(BaseEvent event, Throwable cause, Component failingComponent) {
    super(event, cause, failingComponent);
  }
}
