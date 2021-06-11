/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.notification.legacy;

import java.util.function.Supplier;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.extension.api.notification.NotificationActionDefinition;
import org.mule.runtime.extension.api.notification.NotificationEmitter;

/**
 * Adapts a {@link org.mule.sdk.api.notification.NotificationEmitter} into a legacy {@link NotificationEmitter}
 */
public class LegacyNotificationEmitterAdapter implements NotificationEmitter {

  private final org.mule.sdk.api.notification.NotificationEmitter delegate;

  public LegacyNotificationEmitterAdapter(org.mule.sdk.api.notification.NotificationEmitter delegate) {
    this.delegate = delegate;
  }

  @Override
  public void fire(NotificationActionDefinition action, TypedValue<?> data) {
    delegate.fire(action, data);
  }

  @Override
  public void fireLazy(NotificationActionDefinition action, Supplier<?> dataValue, DataType dataType) {
    delegate.fireLazy(action, dataValue, dataType);
  }
}
