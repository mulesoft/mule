/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.manager;

import java.lang.reflect.Method;

/**
 * Utility for invoking {@link Method methods}.
 * <p>
 * Implementations are free to perform pre/post actions to the invokation, change the target/arguments or even not invoking the
 * method at all.
 *
 * @since 4.2
 */
public interface MethodInvoker {

  /**
   * Used when in need of invoking the {@code method}. Implementations are free to perform pre/post actions to the invokation,
   * change the target/arguments or even not invoking the method at all.
   *
   * @param object the target object
   * @param method the method to be invoked
   * @param args   the supplied arguments
   * @return the return value
   * @throws Throwable if the method fails
   */
  Object invoke(Object object, Method method, Object[] args) throws Throwable;
}
