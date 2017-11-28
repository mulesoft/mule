/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.execution;

import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;

/**
 * Represents the data to be used when firing {@link org.mule.runtime.api.notification.ExtensionNotification} from a source.
 *
 * @since 1.1
 */
public class SourceNotification {

  private final NotificationActionDefinition action;
  private final TypedValue data;

  public SourceNotification(NotificationActionDefinition<?> action, TypedValue<?> data) {
    this.action = action;
    this.data = data;
  }

  public NotificationActionDefinition getAction() {
    return action;
  }

  public TypedValue getData() {
    return data;
  }
}
