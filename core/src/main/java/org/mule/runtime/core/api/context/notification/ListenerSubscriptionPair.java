/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.util.ClassUtils;

import java.util.Optional;

/**
 * A simple tuple that stores a listener with an optional subscription (used to match a resource ID).
 */
public class ListenerSubscriptionPair extends AbstractAnnotatedObject {

  private final ServerNotificationListener listener;
  private final Optional<String> subscription;

  /**
   * For config - must be constructed using the setters
   */
  public ListenerSubscriptionPair() {
    listener = null;
    subscription = empty();
  }

  public ListenerSubscriptionPair(ServerNotificationListener listener) {
    this.listener = listener;
    subscription = empty();
  }

  public ListenerSubscriptionPair(ServerNotificationListener listener, String subscription) {
    this.listener = listener;
    this.subscription = ofNullable(subscription);
  }

  public ServerNotificationListener getListener() {
    return listener;
  }

  public Optional<String> getSubscription() {
    return subscription;
  }

  @Override
  public int hashCode() {
    return ClassUtils.hash(new Object[] {listener, subscription});
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    ListenerSubscriptionPair other = (ListenerSubscriptionPair) obj;
    return ClassUtils.equal(listener, other.listener) && ClassUtils.equal(subscription, other.subscription);
  }

  @Override
  public String toString() {
    return "ListenerSubscriptionPair [listener=" + listener + ", subscription=" + subscription + "]";
  }

}
