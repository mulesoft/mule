/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

/**
 * A command with three parameters that returns an object.
 *
 * @param <R> the return type.
 * @param <A> the type of the first parameter.
 * @param <B> the type of the second parameter.
 * @param <C> the type of the third parameter.
 */
public interface TriCommand<R, A, B, C> {

  /**
   * Executes the command
   *
   * @param firstParameter  the first parameter
   * @param secondParameter the second parameter
   * @param thirdParameter  the third parameter
   *
   * @return the resulting object
   */
  R execute(A firstParameter, B secondParameter, C thirdParameter);

}
