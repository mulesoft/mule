/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

/**
 * A void command with three parameters.
 *
 * @param <A> the type of the first parameter.
 * @param <B> the type of the second parameter
 * @param <C> the type of the third parameter
 */
public interface VoidTriCommand<A, B, C> {

  /**
   * executes the command
   *
   * @param firstParameter  the first parameter.
   * @param secondParameter the second paramater.
   * @param thirdParameter  the third parameter.
   */
  void execute(A firstParameter, B secondParameter, C thirdParameter);

}
