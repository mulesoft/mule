/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.exception;

import org.mule.compatibility.core.connector.EndpointConnectException;
import org.mule.compatibility.core.transport.AbstractConnector;
import org.mule.runtime.core.api.exception.RollbackSourceCallback;
import org.mule.runtime.core.exception.AbstractSystemExceptionStrategy;

import javax.resource.spi.work.Work;

public class ReconnectionAwareSystemExceptionStrategy extends AbstractSystemExceptionStrategy {

  @Override
  public void handleException(Exception ex, RollbackSourceCallback rollbackMethod) {
    super.handleException(ex, rollbackMethod);

    if (ex instanceof EndpointConnectException) {
      handleReconnection((EndpointConnectException) ex);
    }
  }

  protected void handleReconnection(EndpointConnectException ex) {
    final AbstractConnector connector = (AbstractConnector) ex.getFailed();

    // Make sure the connector is not already being reconnected by another receiver thread.
    if (connector.isConnecting()) {
      return;
    }

    logger.info("Exception caught is a EndpointConnectException, attempting to reconnect...");

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
