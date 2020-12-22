/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.internal.execution.NotificationFunction;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.util.List;

/**
 * Augments the {@link SourceCallbackContext} contract with internal behavior we don't want exposed
 * on the public API
 *
 * @since 4.0
 */
public interface SourceCallbackContextAdapter extends SourceCallbackContext {

  /**
   * Releases the bound connection
   */
  void releaseConnection();

  /**
   * Indicates that {@code this} instance has already been used to dispatch an event
   *
   * @since 1.1
   */
  void dispatched();

  /**
   * Retrieves the notification functions.
   *
   * @return a list of {@link NotificationFunction NotificationFunctions} to evaluate and fire
   */
  List<NotificationFunction> getNotificationsFunctions();
}
