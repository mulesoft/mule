/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;

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
   * @param event            the event result of the flow execution
   * @param cause            the cause of the failure
   * @param failingComponent the component that failed
   */
  public FlowExecutionException(CoreEvent event, Throwable cause, Component failingComponent) {
    super(event, cause, failingComponent);
  }
}
