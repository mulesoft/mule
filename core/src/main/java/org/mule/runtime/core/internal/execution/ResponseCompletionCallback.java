/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.internal.exception.MessagingException;

/**
 * Callback to be used to notify of the result of an asynchronous response writing task.
 */
public interface ResponseCompletionCallback {

  /**
   * Notifies that the response was written successfully
   */
  void responseSentSuccessfully();

  /**
   * Notifies that the response writing failed
   *
   * @param e     the exception thrown during response writing
   * @param event the event that was the source for the response.
   * @return the event that is the result of the exception strategy.
   */
  CoreEvent responseSentWithFailure(MessagingException e, CoreEvent event);

}
