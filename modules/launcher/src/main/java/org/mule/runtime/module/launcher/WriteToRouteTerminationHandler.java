/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.launcher;

import static org.mule.runtime.api.util.MuleSystemProperties.MULE_TERMINATION_LOG_ROUTE_PROPERTY;

import static java.lang.System.getProperty;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.FileWriter;
import java.io.IOException;
import java.util.function.Consumer;

import org.slf4j.Logger;

/**
 * A {@link Consumer<Throwable>} that writes the error for the container termination in a termination route.
 *
 * @since 4.7.0
 */
class WriteToRouteTerminationHandler implements Consumer<Throwable> {

  private static final Logger LOGGER = getLogger(DefaultMuleContainer.class);

  private final Consumer<Throwable> shutdownConsumer;

  public WriteToRouteTerminationHandler(Consumer<Throwable> shutdownConsumer) {
    this.shutdownConsumer = shutdownConsumer;
  }

  @Override
  public void accept(Throwable terminationThrowable) {
    String muleTerminationLogRoute = getProperty(MULE_TERMINATION_LOG_ROUTE_PROPERTY);
    if (muleTerminationLogRoute != null) {
      try (FileWriter errorWriter = new FileWriter(muleTerminationLogRoute)) {
        Throwable rootCause = getRootCause(terminationThrowable);
        if (rootCause != null) {
          errorWriter.write(rootCause.getMessage());
        } else {
          errorWriter.write("Unable to write the root cause of ");
        }
      } catch (IOException e) {
        LOGGER.warn("Error on writing termination cause to termination route");
      }
    }
    shutdownConsumer.accept(terminationThrowable);
  }
}
