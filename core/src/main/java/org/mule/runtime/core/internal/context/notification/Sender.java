/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.context.notification;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.core.api.context.notification.ListenerSubscriptionPair;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.core.api.context.notification.NotifierCallback;

import org.slf4j.Logger;

/**
 * This does the work necessary to deliver events to a particular listener. It is generated for a particular {@link Configuration}
 * and stored in a {@link Policy}.
 */
public class Sender<N extends Notification> {

  private static final Logger LOGGER = getLogger(Sender.class);

  private ListenerSubscriptionPair<N> pair;

  Sender(ListenerSubscriptionPair<N> pair) {
    this.pair = pair;
  }

  public void dispatch(N notification, NotifierCallback notifier) {
    if (pair.getSelector().test(notification)) {
      try {
        notifier.notify(pair.getListener(), notification);
      } catch (Throwable e) {
        // Exceptions or errors from listeners do not affect the notification processing
        LOGGER.info("NotificationListener {} was unable to fire notification {} due to an exception: {}.",
                    pair.getListener(), notification, e);
      }
    }
  }

}

