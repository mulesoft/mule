/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util.rx;

import static reactor.core.Exceptions.propagate;

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

  @FunctionalInterface
  public interface CheckedConsumer<T> extends Consumer<T> {

    @Override
    default void accept(T t) {
      try {
        acceptChecked(t);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
    }

    void acceptChecked(T t) throws Throwable;
  }

  @FunctionalInterface
  public interface CheckedBiConsumer<T, U> extends BiConsumer<T, U> {

    @Override
    default void accept(T t, U u) {
      try {
        acceptChecked(t, u);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
    }

    void acceptChecked(T t, U u) throws Throwable;
  }

  @FunctionalInterface
  public interface CheckedFunction<T, R> extends Function<T, R> {

    @Override
    default R apply(T t) {
      try {
        return applyChecked(t);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
    }

    R applyChecked(T t) throws Throwable;
  }

  @FunctionalInterface
  public interface CheckedBiFunction<T, U, R> extends BiFunction<T, U, R> {

    @Override
    default R apply(T t, U u) {
      try {
        return applyChecked(t, u);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
    }

    R applyChecked(T t, U u) throws Throwable;
  }

  @FunctionalInterface
  public interface CheckedPredicate<T> extends Predicate<T> {

    @Override
    default boolean test(T t) {
      try {
        return testChecked(t);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
    }

    boolean testChecked(T t) throws Throwable;
  }

  @FunctionalInterface
  public interface CheckedBiPredicate<T, U> extends BiPredicate<T, U> {

    @Override
    default boolean test(T t, U u) {
      try {
        return testChecked(t, u);
      } catch (Throwable throwable) {
        throw propagate(throwable);
      }
    }

    boolean testChecked(T t, U u) throws Throwable;
  }
}
