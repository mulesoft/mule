/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extensions.jms.api.source;

import org.mule.runtime.api.message.Error;

/**
 * Custom lock implementation to be used in {@link JmsListener} to give the capability to the message listener work
 * synchronously consuming and processing messages through the entire flow.
 *
 * @since 4.0
 * @see JmsListener
 */
public interface JmsListenerLock {

  /**
   * Locks the listener waiting to the message to be processed through the flow
   */
  void lock();

  /**
   * Unlocks the listener indicating that message has been processed correctly
   */
  void unlock();

  /**
   * Unlocks the listener indicating that message has been processed with failures and the error should be propagated
   */
  void unlockWithFailure();

  /**
   * Unlocks the listener indicating that message has been processed with failures and the error should be propagated
   * @param error The error to propagate inside the message listener
   */
  void unlockWithFailure(Error error);

  /**
   * @return the current status of the lock
   */
  boolean isLocked();
}
