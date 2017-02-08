/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.rx;

import static reactor.core.Exceptions.unwrap;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.DefaultMuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.util.func.CheckedBiConsumer;
import org.mule.runtime.core.util.func.CheckedBiFunction;
import org.mule.runtime.core.util.func.CheckedBiPredicate;
import org.mule.runtime.core.util.func.CheckedConsumer;
import org.mule.runtime.core.util.func.CheckedFunction;
import org.mule.runtime.core.util.func.CheckedPredicate;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Utilities for working with checked exceptions with project reactor.
 */
public class Exceptions {

  public static Predicate<Throwable> UNEXPECTED_EXCEPTION_PREDICATE =
      throwable -> !(throwable instanceof EventDroppedException || throwable instanceof MessagingException);

  /**
   * Adapt a {@link CheckedConsumer} to a {@link Consumer} propagating any exceptions thrown by the {@link CheckedConsumer} using
   * {@link reactor.core.Exceptions#propagate(Throwable)}. Useful when using existing methods which throw checked exceptions to
   * avoid try/catch blocks and improve readability.
   *
   * {@code flux.subscribe(Exceptions.checked(v -> checkedMethod(v)}
   *
   * @param checkedConsumer the consumer that throws a checked exception
   * @param <T> the type of the input to the operation
   * @return consumer that adapts {@link CheckedConsumer} and {@link reactor.core.Exceptions#propagate(Throwable)}'s checked
   *         exceptions
   * @see reactor.core.Exceptions#propagate(Throwable)
   */
  public static <T> Consumer<T> checkedConsumer(CheckedConsumer<T> checkedConsumer) {
    return t -> checkedConsumer.accept(t);
  }

  /**
   * Adapt a {@link CheckedBiConsumer} to a {@link BiConsumer} propagating any exceptions thrown by the {@link CheckedBiConsumer}
   * using {@link reactor.core.Exceptions#propagate(Throwable)}. Useful when using existing methods which throw checked exceptions
   * to avoid try/catch blocks and improve readability.
   *
   * @param checkedBiConsumer the biconsumer that throws a chedked exception
   * @param <T> the type of the first argument to the operation
   * @param <U> the type of the second argument to the operation
   * @return biconsumer that adapts {@link CheckedBiConsumer} and {@link reactor.core.Exceptions#propagate(Throwable)}'s checked
   *         exceptions
   * @see reactor.core.Exceptions#propagate(Throwable)
   */
  public static <T, U> BiConsumer<T, U> checkedConsumer(CheckedBiConsumer<T, U> checkedBiConsumer) {
    return (t, u) -> checkedBiConsumer.accept(t, u);
  }

  /**
   * Adapt a {@link CheckedFunction} to a {@link Function} propagating any exceptions thrown by the {@link CheckedFunction} using
   * {@link reactor.core.Exceptions#propagate(Throwable)}. Useful when using existing methods which throw checked exceptions to
   * avoid try/catch blocks and improve readability.
   *
   * {@code flux.map(Exceptions.checked(v -> checkedMethod(v)).subscribe(Subscribers.unbounded())}
   *
   * @param checkedFunction the function that throws a checked exception
   * @param <T> the type of the input to the function
   * @param <R> the type of the result of the function
   * @return function that adapts {@link CheckedFunction} and {@link reactor.core.Exceptions#propagate(Throwable)}'s checked
   *         exceptions
   * @see reactor.core.Exceptions#propagate(Throwable)
   */
  public static <T, R> Function<T, R> checkedFunction(CheckedFunction<T, R> checkedFunction) {
    return t -> checkedFunction.apply(t);
  }

  /**
   * Adapt a {@link CheckedBiFunction} to a {@link BiFunction} propagating any exceptions thrown by the {@link CheckedBiFunction}
   * using {@link reactor.core.Exceptions#propagate(Throwable)}. Useful when using existing methods which throw checked exceptions
   * to avoid try/catch blocks and improve readability.
   *
   * @param checkedBiFunction the bifunction that throws a checked exception
   * @param <T> the type of the first argument to the function
   * @param <U> the type of the second argument to the function
   * @param <R> the type of the result of the function
   * @return bifunction that adapts {@link CheckedBiFunction} and {@link reactor.core.Exceptions#propagate(Throwable)}'s checked
   *         exceptions
   * @see reactor.core.Exceptions#propagate(Throwable)
   */
  public static <T, U, R> BiFunction<T, U, R> checkedFunction(CheckedBiFunction<T, U, R> checkedBiFunction) {
    return (t, u) -> checkedBiFunction.apply(t, u);
  }

  /**
   * Adapt a {@link CheckedPredicate} to a {@link Predicate} propagating any exceptions thrown by the {@link CheckedPredicate}
   * using {@link reactor.core.Exceptions#propagate(Throwable)}. Useful when using existing methods which throw checked exceptions
   * to avoid try/catch blocks and improve readability.
   *
   * {@code flux.filter(Exceptions.checked(v -> checkedMethod(v)).subscribe(Subscribers.unbounded())}
   *
   * @param checkedPredicate the predicate that throws a checked exception
   * @param <T> the type of the input to the predicate
   * @return predicate that adapts {@link CheckedPredicate} and {@link reactor.core.Exceptions#propagate(Throwable)}'s checked
   *         exceptions
   * @see reactor.core.Exceptions#propagate(Throwable)
   */
  public static <T> Predicate<T> checkedPredicate(CheckedPredicate<T> checkedPredicate) {
    return t -> checkedPredicate.test(t);
  }

  /**
   * Adapt a {@link CheckedBiPredicate} to a {@link BiPredicate} propagating any exceptions thrown by the
   * {@link CheckedBiPredicate} using {@link reactor.core.Exceptions#propagate(Throwable)}. Useful when using existing methods
   * which throw checked exceptions to avoid try/catch blocks and improve readability.
   *
   * @param checkedBiPredicate the bipredicate that throws a checked exception
   * @param <T> the type of the first argument to the predicate
   * @param <U> the type of the second argument the predicate
   * @return bipredicate that adapts {@link CheckedBiPredicate} and {@link reactor.core.Exceptions#propagate(Throwable)}'s checked
   *         exceptions
   * @see reactor.core.Exceptions#propagate(Throwable)
   */
  public static <T, U> BiPredicate<T, U> checkedPredicate(CheckedBiPredicate<T, U> checkedBiPredicate) {
    return (t, u) -> checkedBiPredicate.test(t, u);
  }

  /**
   * Unwrap reactive exception and rethrow instances {@link MuleException} or {@link RuntimeException}. Other exception types are
   * wrapped in a instance of {@link DefaultMuleException}.
   *
   * @param throwable
   * @return
   * @throws MuleException
   */
  public static MuleException rxExceptionToMuleException(Throwable throwable)
      throws MuleException {
    throwable = unwrap(throwable);
    if (throwable instanceof MuleException) {
      return (MuleException) throwable;
    } else if (throwable instanceof RuntimeException) {
      throw (RuntimeException) throwable;
    } else {
      throw new DefaultMuleException(throwable);
    }
  }

  /**
   * Returns an exception used to signal a dropped {@link Event} in a reactive stream. This exception does not extend
   * {@link MuleException} or even call {@link Exception#Exception()} to avoid the overhead of filling-in the stack.
   *
   * This exception should only be thrown as part of reactive stream processing and needs to be explicitly handled using for
   * example {@link reactor.core.publisher.Flux#onErrorResumeWith(Predicate, Function)} depending on the behaviuor required in the
   * specific context. For example, in a {@link org.mule.runtime.core.routing.ScatterGatherRouter} a dropped message means one
   * less item in the aggregated collection of messages, while a dropped message in a {@link Flow}
   * means the source should recieve an empty response.
   */
  public static EventDroppedException newEventDroppedException(Event event) {
    return new EventDroppedException(event);
  }

  public static final class EventDroppedException extends Exception {

    private Event dropped;

    private EventDroppedException(Event dropped) {
      this.dropped = dropped;
    }

    public Event getEvent() {
      return this.dropped;
    }
  }

}
