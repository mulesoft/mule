/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

/**
 * A failsafe command with three parameters that returns an object
 *
 * @param <R> the return type
 * @param <A> type of the first parameter
 * @param <B> type of the second parameter
 * @param <C> type of the third parameter
 */
public abstract class AbstractFailsafeTriCommand<R, A, B, C> implements
    TriCommand<R, A, B, C> {

  private final FailsafeTriCommandExecutor<R, A, B, C> failsafeSpanCommand;

  protected AbstractFailsafeTriCommand(Logger logger, String errorMessage, boolean propagateExceptions, R onFailureReturn) {
    failsafeSpanCommand = new FailsafeTriCommandExecutor<>(logger, errorMessage, propagateExceptions, onFailureReturn);
  }

  @Override
  public R execute(A firstParameter, B secondParameter, C thirdParameter) {
    return failsafeSpanCommand.execute(getTriFunction(), firstParameter, secondParameter, thirdParameter);
  }

  abstract TriFunction<A, B, C, R> getTriFunction();
}
