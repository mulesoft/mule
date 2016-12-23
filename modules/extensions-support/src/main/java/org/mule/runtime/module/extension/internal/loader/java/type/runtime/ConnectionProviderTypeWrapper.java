/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import org.mule.runtime.api.connection.ConnectionProvider;
import org.mule.runtime.module.extension.internal.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * {@link TypeWrapper} specification for classes that are considered as Connection Providers
 *
 * @since 4.0
 */
class ConnectionProviderTypeWrapper<T> extends TypeWrapper implements ConnectionProviderElement, ParameterizableTypeElement {

  ConnectionProviderTypeWrapper(Class<T> aClass) {
    super(aClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getSuperClassGenerics() {
    return IntrospectionUtils.getSuperClassGenerics(getDeclaringClass(), ConnectionProvider.class);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Class<?>> getInterfaceGenerics(Class clazz) {
    return IntrospectionUtils.getInterfaceGenerics(getDeclaringClass(), clazz);
  }
}
