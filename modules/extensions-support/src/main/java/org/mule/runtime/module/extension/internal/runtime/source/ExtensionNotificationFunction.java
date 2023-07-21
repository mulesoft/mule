/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.internal.execution.NotificationFunction;

/**
 * An extension specific {@link NotificationFunction} which provides access to the action name that will be fired to allow
 * validations.
 *
 * @since 4.1
 */
public interface ExtensionNotificationFunction extends NotificationFunction {

  /**
   * @return the action that the notification will fire
   */
  String getActionName();

}
