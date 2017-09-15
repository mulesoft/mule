/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.lifecycle;

import static org.reflections.ReflectionUtils.getAllMethods;
import static org.reflections.ReflectionUtils.withName;

import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.core.api.util.ClassUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.Set;

/**
 * Restrict possible results - only OK or a retry based on some throwable are currently allowed.
 */
public final class LifecycleTransitionResult {

  /**
   * The logic for processing a collection of children
   *
   * @param iface The lifecycle interface to be called
   * @param objects An iterator over all children that must also be called
   * @throws LifecycleException if any fail
   */
  private static void processAllNoRetry(Class<? extends Initialisable> iface, Iterator<? extends Initialisable> objects)
      throws LifecycleException {
    if (!iface.isAssignableFrom(Lifecycle.class)) {
      throw new IllegalArgumentException("Not a Lifecycle interface: " + iface);
    }

    Set<Method> lifecycleMethodsCandidate = getAllMethods(iface, withName(Initialisable.PHASE_NAME));
    Method method = lifecycleMethodsCandidate.isEmpty() ? null : lifecycleMethodsCandidate.iterator().next();

    // some interfaces have a single exception, others none
    boolean hasException = method.getExceptionTypes().length > 0;
    Class<?> exception = hasException ? method.getExceptionTypes()[0] : null;

    while (objects.hasNext()) {
      Object target = objects.next();
      processSingleNoRetry(target, method, exception, iface);
    }
  }

  private static void processSingleNoRetry(Object target, Method method, Class<?> exception, Class<?> iface)
      throws LifecycleException {
    if (!iface.isAssignableFrom(target.getClass())) {
      throw new IllegalArgumentException(ClassUtils.getSimpleName(target.getClass()) + " is not an "
          + ClassUtils.getSimpleName(iface));
    }
    try {
      method.invoke(target);
    } catch (IllegalAccessException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Unsupported interface: " + iface).initCause(e);
    } catch (InvocationTargetException e) {
      throw (IllegalArgumentException) new IllegalArgumentException("Unsupported interface: " + iface).initCause(e);
    }
  }

  public static void initialiseAll(Iterator<? extends Initialisable> children) throws InitialisationException {
    try {
      processAllNoRetry(Initialisable.class, children);
    } catch (InitialisationException e) {
      throw e;
    } catch (LifecycleException e) {
      throw new IllegalStateException("Unexpected exception: ", e);
    }
  }
}
