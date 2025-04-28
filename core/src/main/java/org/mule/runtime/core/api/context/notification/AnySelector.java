/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
