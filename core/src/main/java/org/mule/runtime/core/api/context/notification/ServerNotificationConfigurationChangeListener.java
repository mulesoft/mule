/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

/**
 * Listener interface for handling changes to server notification configurations.
 *
 * <p>
 * Implement this interface to define custom behavior when the server's notification configuration is updated.
 * </p>
 */
public interface ServerNotificationConfigurationChangeListener {

  /**
   * Called when there is a change in the server's notification configuration.
   */
  void onServerNotificationConfigurationChange();
}
