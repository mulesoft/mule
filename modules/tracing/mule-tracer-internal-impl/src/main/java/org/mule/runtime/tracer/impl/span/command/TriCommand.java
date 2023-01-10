/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
