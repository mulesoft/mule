/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.execution;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.notification.EnrichedNotificationInfo;

import java.util.Map;

/**
 * Provides a callback to add info entries to an exception just before logging/handling it.
 *
 * When an exception is thrown in a message processor, implementations of this interface will be called in order to augment the
 * exception message with properties that can be helpful to an application developer troubleshooting that exception.
 *
 * @since 3.8.0
 */
public interface ExceptionContextProvider {

  /**
   *
   * @param notificationInfo
   * @return info entries to be added to the logged exception message
   */
  Map<String, Object> getContextInfo(EnrichedNotificationInfo notificationInfo, Component lastProcessed);

}
