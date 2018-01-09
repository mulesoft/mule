/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.core.internal.execution.NotificationFunction;

public interface ExtensionNotificationFunction extends NotificationFunction {

  /**
   * @return the action that the notification will fire
   */
  String getActionName();

}
