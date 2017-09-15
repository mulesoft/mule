/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.context.notification;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;

import java.util.Objects;
import java.util.function.Predicate;

/**
 * A simple tuple that stores a listener with an optional subscription (used to match a resource ID).
 */
public class ListenerSubscriptionPair<N extends Notification> extends AbstractComponent {

  public final static String ANY_SELECTOR_STRING = "*";

  private final NotificationListener<N> listener;
  private final Predicate<N> selector;

  /**
   * For config - must be constructed using the setters
   */
  public ListenerSubscriptionPair() {
    listener = null;
    selector = n -> true;
  }

  public ListenerSubscriptionPair(NotificationListener<N> listener) {
    this.listener = listener;
    selector = n -> true;
  }

  public ListenerSubscriptionPair(NotificationListener<N> listener, Predicate<N> selector) {
    this.listener = listener;
    this.selector = selector;
  }

  public NotificationListener<N> getListener() {
    return listener;
  }

  public Predicate<N> getSelector() {
    return selector;
  }

  @Override
  public int hashCode() {
    return Objects.hash(listener, selector);
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
    return Objects.equals(listener, other.listener) && Objects.equals(selector, other.selector);
  }

  @Override
  public String toString() {
    return "ListenerSubscriptionPair [listener=" + listener + ", selector=" + selector + "]";
  }

}
