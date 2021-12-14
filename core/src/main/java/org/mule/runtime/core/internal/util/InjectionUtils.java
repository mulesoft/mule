/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util;

import org.mule.runtime.core.internal.registry.InjectionTargetDecorator;

public final class InjectionUtils {

  private InjectionUtils() {}

  public static <T> T getInjectionTarget(T object) {
    while (object instanceof InjectionTargetDecorator) {
      object = ((InjectionTargetDecorator<T>) object).getDelegate();
    }

    return object;
  }
}
