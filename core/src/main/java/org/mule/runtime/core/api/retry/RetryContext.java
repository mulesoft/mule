/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.notification.NotificationDispatcher;

import java.util.Map;

/**
 * The RetryContext is used to store any data which carries over from attempt to attempt such as response messages.
 */
public interface RetryContext {

  String FAILED_RECEIVER = "failedReceiver";
  String FAILED_DISPATCHER = "failedDispatcher";
  String FAILED_REQUESTER = "failedRequester";

  /**
   * @return a read-only meta-info map or an empty map, never null.
   */
  Map<Object, Object> getMetaInfo();

  Message[] getReturnMessages();

  Message getFirstReturnMessage();

  void setReturnMessages(Message[] returnMessages);

  void addReturnMessage(Message result);

  String getDescription();

  NotificationDispatcher getNotificationFirer();

  /**
   * The most recent failure which prevented the context from validating the connection. Note that the method may return null.
   * Instead, the {@link #isOk()} should be consulted first.
   *
   * @return last failure or null
   */
  Throwable getLastFailure();

  /**
   * Typically called by validation logic to mark no problems with the current connection. Additionally, clears any previous
   * failure set.
   */
  void setOk();

  /**
   * Typically called by validation logic to mark a problem and an optional root cause.
   *
   * @param lastFailure the most recent failure, can be null
   */
  void setFailed(Throwable lastFailure);

  /**
   * Note that it's possible for an implementation to return false and have no failure specified, thus the subsequent
   * {@link #getLastFailure()} may return null.
   *
   * @return true if no problems detected before
   */
  boolean isOk();
}
