/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.tracer.impl.span.command;

import org.apache.commons.lang3.function.TriFunction;
import org.slf4j.Logger;

/**
 * A failsafe void command with three parameters.
 *
 * @param <A> the type of the first parameter.
 * @param <B> the type of the second parameter
 * @param <C> the type of the third parameter.
 */
public abstract class AbstractFailSafeVoidTriCommand<A, B, C> implements VoidTriCommand<A, B, C> {

  private final FailsafeTriCommandExecutor<Void, A, B, C> failsafeSpanCommand;

  protected AbstractFailSafeVoidTriCommand(Logger logger, String errorMessage, boolean propagateExceptions) {
    failsafeSpanCommand = new FailsafeTriCommandExecutor(logger, errorMessage, propagateExceptions, Void.TYPE);
  }

  @Override
  public void execute(A firstParameter, B secondParameter, C thirdParameter) {
    failsafeSpanCommand.execute(getTriConsumer(), firstParameter, secondParameter, thirdParameter);
  }

  abstract TriFunction<A, B, C, Void> getTriConsumer();
}
