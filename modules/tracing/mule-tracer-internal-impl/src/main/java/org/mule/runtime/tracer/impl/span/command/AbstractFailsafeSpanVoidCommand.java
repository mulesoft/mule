/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.method;

import org.slf4j.Logger;

import static org.mule.runtime.tracer.impl.span.method.FailsafeSpanCommand.getFailsafeSpanOperation;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An abstract implementation of {@link VoidCommand} related to {@link org.mule.runtime.tracer.api.span.InternalSpan}
 *
 * @since 4.5.0
 */
public abstract class AbstractFailSafeSpanVoidCommand implements VoidCommand {

  private static final Logger LOGGER = getLogger(AbstractFailSafeSpanVoidCommand.class);

  private final FailsafeSpanCommand failSafeSpanCommand =
    getFailsafeSpanOperation(LOGGER, getErrorMessage(), true);

  @Override
  public void execute() {
    failSafeSpanCommand
      .execute(() -> getRunnable().run());
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
