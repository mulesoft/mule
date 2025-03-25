/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mockito.Mock;
import org.mule.runtime.api.util.Pair;

import javax.inject.Inject;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Simplified injection utility for tests. Injects mocks set up on the 'test' object (expected to be the test class...) in a
 * target by looking for 'Inject' annotated fields.
 *
 * If there's not a suitable mock, there's no injection. N.B. does not currently look at 'extra interfaces' for the mocks.
 */
public class MockInjector {

  /**
   * Inject fields marked with '@Mock' on the 'test' object into any fields marked with 'Inject' on the target. This walks up the
   * class hierarchy in both cases, so mocks defined on a superclass of 'test' will be injected, and fields marked with 'Inject'
   * on superclasses of 'target' will be checked.
   *
   * Note: no mock, no effect. If the test doesn't have a field that has a type that is assignable to the injected field, no
   * injection will happen.
   *
   * @param test   the object with the mocks (usually a test class).
   * @param target the object to inject values into.
   */
  public static void injectMocksFromSuite(Object test, Object target) {
    List<Object> candidates = getClassesInHierarchy(test).stream()
        .flatMap(c -> Arrays.stream(c.getDeclaredFields()))
        .filter(f -> f.getAnnotation(Mock.class) != null)
        .peek(f -> f.setAccessible(true))
        .map(f -> {
          try {
            return f.get(test);
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        })
        .toList();
    getClassesInHierarchy(target).stream()
        .flatMap(c -> Arrays.stream(c.getDeclaredFields()))
        .filter(f -> f.getAnnotation(Inject.class) != null)
        .peek(f -> f.setAccessible(true))
        .map(f -> new Pair<>(f, candidates.stream().filter(o -> f.getType().isAssignableFrom(o.getClass())).findFirst()))
        .filter(p -> p.getSecond().isPresent())
        .forEach(p -> {
          try {
            p.getFirst().set(target, p.getSecond().get());
          } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
          }
        });
  }

  /**
   * Walk up the class hierarchy and return a list of classes in hierarchy order. Stops at Object.class (which won't be included)
   *
   * @param base object to get hierarchy for...
   * @return list of Class&lt;?&gt; objects
   */
  public static List<Class> getClassesInHierarchy(Object base) {
    final List<Class> classes = new ArrayList<>();
    for (Class<?> c = base.getClass(); !c.equals(Object.class); c = c.getSuperclass()) {
      classes.add(c);
    }
    return classes;
  }
}
