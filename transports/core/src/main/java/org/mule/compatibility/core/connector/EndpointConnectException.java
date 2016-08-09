/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.connector;

import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.compatibility.core.transport.AbstractTransportMessageHandler;
import org.mule.runtime.core.api.connector.Connectable;
import org.mule.runtime.core.config.i18n.Message;
import org.mule.runtime.core.connector.ConnectException;

import javax.resource.spi.work.Work;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When this exception is thrown it will trigger a retry (reconnection) policy to go into effect if one is configured.
 */
public class EndpointConnectException extends ConnectException {

  protected static Logger logger = LoggerFactory.getLogger(EndpointConnectException.class);

  /** Serial version */
  private static final long serialVersionUID = -7802483584780922653L;

  public EndpointConnectException(Message message, Connectable failed) {
    super(message, resolveFailed(failed));
  }

  public EndpointConnectException(Message message, Throwable cause, Connectable failed) {
    super(message, cause, resolveFailed(failed));
  }

  public EndpointConnectException(Throwable cause, Connectable failed) {
    super(cause, resolveFailed(failed));
  }

  protected static Connectable resolveFailed(Connectable failed) {
    return failed instanceof AbstractTransportMessageHandler ? ((AbstractTransportMessageHandler) failed).getConnector() : failed;
  }

  @Override
  public void handleReconnection() {
    final AbstractConnector connector = (AbstractConnector) getFailed();

    // Make sure the connector is not already being reconnected by another receiver thread.
    if (connector.isConnecting()) {
      return;
    }

    logger.info("Exception caught is a ConnectException, attempting to reconnect...");

    // Disconnect
    try {
      logger.debug("Disconnecting " + connector.getName());
      connector.stop();
      connector.disconnect();
    } catch (Exception e1) {
      logger.error(e1.getMessage());
    }

    // Reconnect (retry policy will go into effect here if configured)
    try {
      connector.getMuleContext().getWorkManager().scheduleWork(new Work() {

        @Override
        public void release() {}

        @Override
        public void run() {
          try {
            logger.debug("Reconnecting " + connector.getName());
            connector.start();
          } catch (Exception e) {
            if (logger.isDebugEnabled()) {
              logger.debug("Error reconnecting", e);
            }
            logger.error(e.getMessage());
          }
        }
      });
    } catch (Exception e) {
      if (logger.isDebugEnabled()) {
        logger.debug("Error executing reconnect work", e);
      }
      logger.error(e.getMessage());
    }
  }
}
