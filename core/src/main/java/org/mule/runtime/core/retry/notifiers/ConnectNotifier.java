/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.retry.notifiers;

import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.config.ExceptionHelper;
import org.mule.runtime.core.context.notification.ConnectionNotification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires a {@link ConnectionNotification} each time a retry attempt is made.
 */
public class ConnectNotifier implements RetryNotifier {

  protected transient final Logger logger = LoggerFactory.getLogger(ConnectNotifier.class);

  public void onSuccess(RetryContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug("Successfully connected to " + context.getDescription());
    }

    fireConnectNotification(ConnectionNotification.CONNECTION_CONNECTED, context.getDescription(), context);
  }

  public void onFailure(RetryContext context, Throwable e) {
    fireConnectNotification(ConnectionNotification.CONNECTION_FAILED, context.getDescription(), context);

    if (logger.isErrorEnabled()) {
      StringBuilder msg = new StringBuilder(512);
      msg.append("Failed to connect/reconnect: ").append(context.getDescription());
      Throwable t = ExceptionHelper.getRootException(e);
      msg.append(". Root Exception was: ").append(ExceptionHelper.writeException(t));
      if (logger.isTraceEnabled()) {
        t.printStackTrace();
      }
      logger.error(msg.toString());
    }
  }

  protected void fireConnectNotification(int action, String description, RetryContext context) {
    context.getMuleContext().fireNotification(new ConnectionNotification(null, description, action));
  }
}
