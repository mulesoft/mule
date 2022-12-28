/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import java.util.function.BiConsumer;

import org.slf4j.Logger;

/**
 * A failsafe void command with two parameters.
 *
 * @param <A> the type of the first parameter
 * @param <B> the type of the second parameter
 */
public abstract class AbstractFailSafeVoidBiCommand<A, B> implements VoidBiCommand<A, B> {

  private final FailsafeBiCommandExecutor<A, B> failsafeSpanCommand;

  protected AbstractFailSafeVoidBiCommand(Logger logger, String errorMessage, boolean propagateExceptions) {
    failsafeSpanCommand = new FailsafeBiCommandExecutor<>(logger, errorMessage, propagateExceptions);
  }

  @Override
  public void execute(A firstParameter, B secondParameter) {
    failsafeSpanCommand.execute(getConsumer(), firstParameter, secondParameter);
  }

  abstract BiConsumer<A, B> getConsumer();
}
