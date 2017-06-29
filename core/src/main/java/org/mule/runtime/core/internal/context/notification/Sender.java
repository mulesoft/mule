/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.core.api.context.notification.NotifierCallback;
import org.mule.runtime.core.api.context.notification.ServerNotification;

/**
 * This does the work necessary to deliver events to a particular listener. It is generated for a particular {@link Configuration}
 * and stored in a {@link Policy}.
 */
public class Sender {

  private ListenerSubscriptionPair pair;

  Sender(ListenerSubscriptionPair pair) {
    this.pair = pair;
  }

  public void dispatch(ServerNotification notification, NotifierCallback notifier) {
    if (!pair.getSubscription().isPresent()
        || (null != notification.getResourceIdentifier()
            && pair.getSubscription().get().equalsIgnoreCase(notification.getResourceIdentifier()))) {
      try {
        notifier.notify(pair.getListener(), notification);
      } catch (Exception e) {
        // Exceptions from listeners do not affect the notification processing
      }
    }
  }

}

