/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.context.notification;

import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.api.context.notification.NotifierCallback;

/**
 * This does the work necessary to deliver events to a particular listener. It is generated for a particular {@link Configuration}
 * and stored in a {@link Policy}.
 */
public class Sender<N extends Notification> {

  private ListenerSubscriptionPair<N> pair;

  Sender(ListenerSubscriptionPair<N> pair) {
    this.pair = pair;
  }

  public void dispatch(N notification, NotifierCallback notifier) {
    if (pair.getSelector().test(notification)) {
      notifier.notify(pair.getListener(), notification);
    }
  }

  NotificationListener<N> getListener() {
    return pair.getListener();
  }
}

