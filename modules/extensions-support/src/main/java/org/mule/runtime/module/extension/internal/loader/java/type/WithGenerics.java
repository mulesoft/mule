/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type;

import java.lang.reflect.Type;
import java.util.List;

/**
 * A generic contract for any kind of component that could contain generic for their extended Super Class or implemented interface
 *
 * @since 4.0
 */
public interface WithGenerics {

  /**
   * @return A list of {@link Type} generic that the super class holds
   */
  List<Type> getSuperClassGenerics();

  /**
   * @param clazz Interface class to look for their generics
   * @return A list of {@link Class} generic that the implemented interface, indicated by {@code clazz}, holds
   */
  List<Class<?>> getInterfaceGenerics(Class clazz);
}
