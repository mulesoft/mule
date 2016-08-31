/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model.runtime;

import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.introspection.describer.model.SourceElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.reflect.Type;
import java.util.List;

/**
 * {@link TypeWrapper} specification for {@link Source} types.
 *
 * @since 4.0
 */
final class SourceTypeWrapper<T extends Source> extends TypeWrapper implements SourceElement, ParameterizableTypeElement {

  private final Class<T> aClass;

  SourceTypeWrapper(Class<T> aClass) {
    super(aClass);
    this.aClass = aClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<? extends Source> getDeclaringClass() {
    return aClass;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getSuperClassGenerics() {
    return IntrospectionUtils.getSuperClassGenerics(aClass, Source.class);
  }
}
