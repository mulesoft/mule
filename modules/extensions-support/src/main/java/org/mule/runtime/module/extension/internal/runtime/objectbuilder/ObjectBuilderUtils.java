/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.objectbuilder;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.util.ClassUtils;

/**
 * Utilities to power {@link ObjectBuilder} implementations
 *
 * @since 4.0
 */
class ObjectBuilderUtils {

  /**
   * Creates a new instance of the given {@code prototypeClass}
   *
   * @param prototypeClass the class of the object to create
   * @param <T>            the generic type of the {@code prototypeClass}
   * @return a new instance
   */
  public static <T> T createInstance(Class<T> prototypeClass) {
    try {
      return withContextClassLoader(prototypeClass.getClassLoader(), () -> ClassUtils.instantiateClass(prototypeClass));
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Could not create instance of " + prototypeClass), e);
    }
  }

  private ObjectBuilderUtils() {}
}
