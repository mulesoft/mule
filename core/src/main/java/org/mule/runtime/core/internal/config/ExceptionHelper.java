/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.config;

import org.mule.runtime.api.legacy.exception.ExceptionReader;

import java.util.LinkedList;

/**
 * <code>ExceptionHelper</code> provides a number of helper functions that can be useful for dealing with Mule exceptions.
 */
public final class ExceptionHelper extends org.mule.runtime.api.exception.ExceptionHelper {

  /**
   * Do not instantiate.
   */
  private ExceptionHelper() {
    super();
  }

  public static <T> T traverseCauseHierarchy(Throwable e, ExceptionEvaluator<T> evaluator) {
    LinkedList<Throwable> exceptions = new LinkedList<>();
    exceptions.add(e);
    while (e.getCause() != null && !e.getCause().equals(e)) {
      exceptions.addFirst(e.getCause());
      e = e.getCause();
    }
    for (Throwable exception : exceptions) {
      T value = evaluator.evaluate(exception);
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  public static String writeException(Throwable t) {
    ExceptionReader er = getExceptionReader(t);
    StringBuilder msg = new StringBuilder();
    msg.append(er.getMessage(t)).append(". Type: ").append(t.getClass());
    return msg.toString();
  }

  public interface ExceptionEvaluator<T> {

    T evaluate(Throwable e);
  }
}
