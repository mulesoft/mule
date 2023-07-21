/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.AbstractServerNotification;
import org.mule.runtime.api.notification.Notification;

import java.util.function.Predicate;

/**
 * Notification listener selector that matches a notification form a component with a given identifier.
 * 
 * @since 4.5
 */
public final class ResourceIdentifierSelector implements Predicate<Notification> {

  private final String subscription;

  public ResourceIdentifierSelector(String subscription) {
    this.subscription = subscription;
  }

  @Override
  public boolean test(Notification notification) {
    return subscription != null ? subscription
        .equals(((AbstractServerNotification) notification).getResourceIdentifier()) : true;
  }

  @Override
  public String toString() {
    return "selector(" + subscription + ")";
  }
}
