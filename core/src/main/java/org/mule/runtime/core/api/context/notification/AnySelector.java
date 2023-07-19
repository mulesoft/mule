/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.notification.Notification;

import java.util.function.Predicate;

/**
 * Notification listener selector that matches any notification
 * 
 * @since 4.5
 */
public final class AnySelector implements Predicate<Notification> {

  public static final Predicate<? extends Notification> ANY_SELECTOR = new AnySelector();

  @Override
  public boolean test(Notification t) {
    return true;
  }

  @Override
  public String toString() {
    return "selector(*)";
  }
}
