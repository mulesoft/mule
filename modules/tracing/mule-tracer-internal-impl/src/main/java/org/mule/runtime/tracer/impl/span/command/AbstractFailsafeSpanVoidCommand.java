/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import static org.mule.runtime.api.util.MuleSystemProperties.ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING;
import static org.mule.runtime.tracer.impl.span.command.FailsafeSpanCommand.getFailsafeSpanOperation;

import static java.lang.Boolean.getBoolean;

import static org.slf4j.LoggerFactory.getLogger;

import org.slf4j.Logger;

/**
 * An abstract implementation of {@link VoidCommand} related to {@link org.mule.runtime.tracer.api.span.InternalSpan}
 *
 * @since 4.5.0
 */
public abstract class AbstractFailsafeSpanVoidCommand implements VoidCommand {

  private static final Logger LOGGER = getLogger(AbstractFailsafeSpanVoidCommand.class);

  private final FailsafeSpanCommand failSafeSpanCommand =
      getFailsafeSpanOperation(LOGGER, getErrorMessage(), getBoolean(ENABLE_PROPAGATION_OF_EXCEPTIONS_IN_TRACING));

  @Override
  public void execute() {
    failSafeSpanCommand.execute(getRunnable());
  }

  /**
   * @return the runnable to execute the command
   */
  protected abstract Runnable getRunnable();

  /**
   * @return the error message in case of failure.
   */
  protected abstract String getErrorMessage();

}
