/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.introspection.describer.model.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodsAnnotatedWith;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.module.extension.internal.introspection.describer.model.MethodElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.ParameterizableTypeElement;
import org.mule.runtime.module.extension.internal.introspection.describer.model.SourceElement;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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

  @Override
  public Optional<MethodElement> getOnResponseMethod() {
    return getMethodAnnotatedWith(OnSuccess.class);
  }

  @Override
  public Optional<MethodElement> getOnErrorMethod() {
    return getMethodAnnotatedWith(OnError.class);
  }

  private Optional<MethodElement> getMethodAnnotatedWith(Class<? extends Annotation> annotationType) {
    Collection<Method> methods = getMethodsAnnotatedWith(aClass, annotationType);
    if (methods.isEmpty()) {
      return empty();
    } else if (methods.size() > 1) {
      throw new IllegalSourceModelDefinitionException(
                                                      format("Source declared in class '%s' declares more than one method annotated with '%s'",
                                                             aClass.getName(), annotationType.getSimpleName()));
    } else {
      return of(new MethodWrapper(methods.iterator().next()));
    }
  }
}
