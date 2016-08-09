/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static java.util.Arrays.stream;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static org.apache.commons.lang.SystemUtils.LINE_SEPARATOR;
import org.mule.runtime.api.connection.ConnectionException;

import java.util.List;
import java.util.ListIterator;
import java.util.Optional;
import java.util.concurrent.Callable;

/**
 * Mule exception utilities.
 */
public class ExceptionUtils extends org.apache.commons.lang.exception.ExceptionUtils {

  /**
   * This method returns true if the throwable contains a {@link Throwable} that matches the specified class or subclass in the
   * exception chain. Subclasses of the specified class do match.
   *
   * @param throwable the throwable to inspect, may be null
   * @param type the type to search for, subclasses match, null returns false
   * @return the index into the throwable chain, false if no match or null input
   */
  public static boolean containsType(Throwable throwable, Class<?> type) {
    return indexOfType(throwable, type) > -1;
  }

  /**
   * This method returns the throwable closest to the root cause that matches the specified class or subclass. Any null argument
   * will make the method return null.
   *
   * @param throwable the throwable to inspect, may be null
   * @param type the type to search for, subclasses match, null returns null
   * @return the throwable that is closest to the root in the throwable chain that matches the type or subclass of that type.
   */
  @SuppressWarnings("unchecked")
  public static <ET> ET getDeepestOccurenceOfType(Throwable throwable, Class<ET> type) {
    if (throwable == null || type == null) {
      return null;
    }
    List<Throwable> throwableList = getThrowableList(throwable);
    ListIterator<Throwable> listIterator = throwableList.listIterator(throwableList.size());
    while (listIterator.hasPrevious()) {
      Throwable candidate = listIterator.previous();
      if (type.isAssignableFrom(candidate.getClass())) {
        return (ET) candidate;
      }
    }
    return null;
  }

  /**
   * Similar to {@link #getFullStackTrace(Throwable)} but removing the exception and causes messages. This is useful to determine
   * if two exceptions have matching stack traces regardless of the messages which may contain invokation specific data
   *
   * @param throwable the throwable to inspect, may be <code>null</code>
   * @return the stack trace as a string, with the messages stripped out. Empty string if throwable was <code>null</code>
   */
  public static String getFullStackTraceWithoutMessages(Throwable throwable) {
    StringBuilder builder = new StringBuilder();

    for (String frame : getStackFrames(throwable)) {
      builder.append(frame.replaceAll(":\\s+([\\w\\s]*.*)", "").trim()).append(LINE_SEPARATOR);
    }

    return builder.toString();
  }

  /**
   * Introspects the {@link Throwable} parameter to obtain the first {@link Throwable} of type {@link ConnectionException} in the
   * exception chain.
   *
   * @param throwable the last throwable in the exception chain.
   * @return an {@link Optional} value with the first {@link ConnectionException} in the exception chain if any.
   */
  public static Optional<ConnectionException> extractConnectionException(Throwable throwable) {
    return extractOfType(throwable, ConnectionException.class);
  }

  /**
   * Introspects the {@link Throwable} parameter to obtain the first {@link Throwable} of type {@code throwableType} in the
   * exception chain and return the cause of it.
   *
   * @param throwable the last throwable on the exception chain.
   * @param throwableType the type of the throwable that the cause is wanted.
   * @return the cause of the first {@link Throwable} of type {@code throwableType}.
   */
  public static Optional<Throwable> extractCauseOfType(Throwable throwable, Class<? extends Throwable> throwableType) {
    Optional<? extends Throwable> typeThrowable = extractOfType(throwable, throwableType);
    return typeThrowable.isPresent() ? ofNullable(typeThrowable.get().getCause()) : empty();
  }

  /**
   * Introspects the {@link Throwable} parameter to obtain the first {@link Throwable} of type {@code throwableType} in the
   * exception chain.
   * <p>
   * This method handles recursive cause structures that might otherwise cause infinite loops. If the throwable parameter is a
   * {@link ConnectionException} the same value will be returned. If the throwable parameter has a cause of itself, then an empty
   * value will be returned.
   *
   * @param throwable the last throwable on the exception chain.
   * @param throwableType the type of the throwable is wanted to find.
   * @return the cause of the first {@link Throwable} of type {@code throwableType}.
   */
  @SuppressWarnings("unchecked")
  public static <T extends Throwable> Optional<T> extractOfType(Throwable throwable, Class<T> throwableType) {
    if (throwable == null || !containsType(throwable, throwableType)) {
      return empty();
    }

    return (Optional<T>) stream(getThrowables(throwable)).filter(throwableType::isInstance).findFirst();
  }

  /**
   * Executes the given {@code callable} knowing that it might throw an {@link Exception} of type {@code expectedExceptionType}.
   * If that happens, then it will re throw such exception.
   * <p>
   * If the {@code callable} throws a {@link RuntimeException} of a different type, then it is also re-thrown. Finally, if an
   * exception of any different type is thrown, then it is handled by delegating into the {@code exceptionHandler}, which might in
   * turn also throw an exception or handle it returning a value.
   *
   * @param expectedExceptionType the type of exception which is expected to be thrown
   * @param callable the delegate to be executed
   * @param exceptionHandler a {@link ExceptionHandler} in case an unexpected exception is found instead
   * @param <T> the generic type of the return value
   * @param <E> the generic type of the expected exception
   * @return a value returned by either the {@code callable} or the {@code exceptionHandler}
   * @throws E if the expected exception is actually thrown
   */
  public static <T, E extends Exception> T tryExpecting(Class<E> expectedExceptionType, Callable<T> callable,
                                                        ExceptionHandler<T, E> exceptionHandler)
      throws E {
    try {
      return callable.call();
    } catch (Exception e) {
      if (expectedExceptionType.isInstance(e)) {
        throw (E) e;
      }

      if (e instanceof RuntimeException) {
        throw (RuntimeException) e;
      }

      return exceptionHandler.handle(e);
    }
  }
}
