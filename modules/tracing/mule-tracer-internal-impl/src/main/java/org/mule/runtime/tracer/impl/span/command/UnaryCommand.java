/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
