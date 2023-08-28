/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.api.retry.policy;

import static org.mule.runtime.api.notification.ConnectionNotification.CONNECTION_CONNECTED;
import static org.mule.runtime.api.notification.ConnectionNotification.CONNECTION_FAILED;

import org.mule.runtime.api.notification.ConnectionNotification;
import org.mule.runtime.core.api.retry.RetryContext;
import org.mule.runtime.core.api.retry.RetryNotifier;
import org.mule.runtime.core.internal.config.ExceptionHelper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fires a {@link ConnectionNotification} each time a retry attempt is made.
 */
public final class ConnectNotifier implements RetryNotifier {

  protected transient final Logger logger = LoggerFactory.getLogger(ConnectNotifier.class);

  @Override
  public void onSuccess(RetryContext context) {
    if (logger.isDebugEnabled()) {
      logger.debug("Successfully connected to " + context.getDescription());
    }

    fireConnectNotification(CONNECTION_CONNECTED, context.getDescription(), context);
  }

  @Override
  public void onFailure(RetryContext context, Throwable e) {
    fireConnectNotification(CONNECTION_FAILED, context.getDescription(), context);

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
    context.getNotificationFirer().dispatch(new ConnectionNotification(null, description, action));
  }
}
