/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling.internal.artifact.context;

import static java.lang.System.currentTimeMillis;
import static java.util.Objects.requireNonNull;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.module.extension.internal.ExtensionResolvingContext;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An decorator for a {@link ExtensionResolvingContext} in order to provide logging for configuration and connections.
 */
public class LoggingResolvingContext {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoggingResolvingContext.class);

  private ExtensionResolvingContext extensionResolvingContext;

  public LoggingResolvingContext(ExtensionResolvingContext extensionResolvingContext) {
    requireNonNull(extensionResolvingContext, "extensionResolvingContext cannot be null");

    this.extensionResolvingContext = extensionResolvingContext;
  }

  /**
   * @param <C> Configuration type
   * @return optional configuration of a component
   */
  public <C> Optional<C> getConfig() {
    long startTime = currentTimeMillis();
    try {
      return extensionResolvingContext.getConfig();
    } finally {
      withContextClassLoader(this.getClass().getClassLoader(), () -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Getting config took [{}ms]", currentTimeMillis() - startTime);
        }
      });
    }
  }

  /**
   * Retrieves the connection for the related component and configuration
   *
   * @param <C> Connection type
   * @return A connection instance of {@param <C>} type for the component. If the related configuration does not require a
   * connection {@link Optional#empty()} will be returned
   * @throws ConnectionException when no valid connection is found for the related component and configuration
   */
  public <C> Optional<C> getConnection() throws ConnectionException {
    long startTime = currentTimeMillis();
    try {
      return extensionResolvingContext.getConnection();
    } finally {
      withContextClassLoader(this.getClass().getClassLoader(), () -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Getting connection took [{}ms]", currentTimeMillis() - startTime);
        }
      });
    }
  }

  /**
   * {@inheritDoc}
   */
  public void dispose() {
    long startTime = currentTimeMillis();
    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Disposing context");
    }
    try {
      extensionResolvingContext.dispose();
    } finally {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Dispose took [{}ms]", currentTimeMillis() - startTime);
      }
    }
  }

}
