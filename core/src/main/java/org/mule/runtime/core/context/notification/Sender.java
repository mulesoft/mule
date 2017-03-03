/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.context.notification;

import org.mule.runtime.core.api.context.notification.ServerNotification;
import org.mule.runtime.core.routing.filters.WildcardFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This does the work necessary to deliver events to a particular listener. It is generated for a particular {@link Configuration}
 * and stored in a {@link org.mule.runtime.core.context.notification.Policy}.
 */
class Sender {

  private static final Logger LOGGER = LoggerFactory.getLogger(Sender.class);

  private ListenerSubscriptionPair pair;
  private WildcardFilter subscriptionFilter;

  Sender(ListenerSubscriptionPair pair) {
    this.pair = pair;
    subscriptionFilter = new WildcardFilter(pair.getSubscription());
    subscriptionFilter.setCaseSensitive(false);
  }

  public void dispatch(ServerNotification notification, NotifierCallback notifier) {
    if (pair.isNullSubscription()
        || (null != notification.getResourceIdentifier() && subscriptionFilter.accept(notification.getResourceIdentifier()))) {
      try {
        notifier.notify(pair.getListener(), notification);
      } catch (RuntimeException e) {
        // Exceptions from listeners do not affect the notification processing
		LOGGER.error("Ignore " + e.getClass() + " " + e + " on " + notifier);
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("ExceptionDetails on " + notifier, e);
		}
      }
    }
  }

}

