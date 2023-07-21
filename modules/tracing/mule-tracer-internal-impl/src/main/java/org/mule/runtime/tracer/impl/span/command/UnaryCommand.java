/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.tracer.impl.span.command;

/**
 * A command with one parameter that returns an object.
 *
 * @param <A> the type of the first parameter.
 * @param <R> the return type.
 */
public interface UnaryCommand<A, R> {

  /**
   * Executes the command
   *
   * @param parameter the parameter
   *
   * @return the resulting object
   */
  R execute(A parameter);
}
