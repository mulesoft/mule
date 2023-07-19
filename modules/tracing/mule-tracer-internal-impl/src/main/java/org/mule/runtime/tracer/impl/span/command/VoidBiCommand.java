/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

/**
 * A void command with two parameters.
 *
 * @param <A> the type of the first parameter.
 * @param <B> the type of the second parameter
 */
public interface VoidBiCommand<A, B> {

  /**
   * executes the command
   *
   * @param firstParameter  the first parameter
   * @param secondParameter the second parameter.
   */
  void execute(A firstParameter, B secondParameter);

}
