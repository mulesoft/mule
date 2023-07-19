/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
