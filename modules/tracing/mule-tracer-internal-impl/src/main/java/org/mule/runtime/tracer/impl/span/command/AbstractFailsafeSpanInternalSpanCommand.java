/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.mule.runtime.tracer.api.span.InternalSpan;
import org.slf4j.Logger;

import java.util.Optional;
import java.util.function.Supplier;

import static org.mule.runtime.tracer.impl.span.command.FailsafeSpanCommand.getFailsafeSpanOperation;
import static org.slf4j.LoggerFactory.getLogger;

/**
 * An abstract implementation of {@link Command} related to {@link org.mule.runtime.tracer.api.span.InternalSpan}
 *
 * @since 4.5.0
 */
public abstract class AbstractFailsafeSpanInternalSpanCommand implements Command<Optional<InternalSpan>> {

  private static final Logger LOGGER = getLogger(AbstractFailsafeSpanVoidCommand.class);

  private final FailsafeSpanCommand failSafeSpanCommand =
      getFailsafeSpanOperation(LOGGER, getErrorMessage(), true);

  @Override
  public Optional<InternalSpan> execute() {
    return failSafeSpanCommand.execute(getSupplier());
  }

  /**
   * @return the supplier of {@link InternalSpan}
   */
  protected abstract Supplier<Optional<InternalSpan>> getSupplier();

  /**
   * @return the error message in case of failure.
   */
  protected abstract String getErrorMessage();

}
