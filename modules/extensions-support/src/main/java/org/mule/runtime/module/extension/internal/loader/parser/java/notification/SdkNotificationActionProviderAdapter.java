/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java.notification;

import static java.util.stream.Collectors.toCollection;

import org.mule.sdk.api.annotation.notification.NotificationActionProvider;
import org.mule.sdk.api.notification.NotificationActionDefinition;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Adapts a {@link org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider} into a
 * {@link NotificationActionProvider}
 *
 * @since 4.5.0
 */
public class SdkNotificationActionProviderAdapter implements NotificationActionProvider {

  public static NotificationActionProvider from(Object provider) {
    if (provider instanceof NotificationActionProvider) {
      return (NotificationActionProvider) provider;
    } else if (provider instanceof org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider) {
      return new SdkNotificationActionProviderAdapter((org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider) provider);
    } else {
      throw new IllegalArgumentException("Invalid type " + provider.getClass());
    }
  }

  private final org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider delegate;

  public SdkNotificationActionProviderAdapter(org.mule.runtime.extension.api.annotation.notification.NotificationActionProvider delegate) {
    this.delegate = delegate;
  }

  @Override
  public Set<NotificationActionDefinition> getNotificationActions() {
    return delegate.getNotificationActions().stream()
        .map(SdkNotificationActionDefinitionAdapter::from)
        .collect(toCollection(LinkedHashSet::new));
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof SdkNotificationActionProviderAdapter) {
      return delegate.equals(((SdkNotificationActionProviderAdapter) o).delegate);
    }

    return delegate.equals(o);
  }

  @Override
  public int hashCode() {
    return delegate.hashCode();
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
