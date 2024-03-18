/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getMethodsAnnotatedWith;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.annotation.source.OnBackPressure;
import org.mule.runtime.extension.api.exception.IllegalSourceModelDefinitionException;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.ParameterizableTypeElement;
import org.mule.runtime.module.extension.api.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * {@link TypeWrapper} specification for {@link Source} types.
 *
 * @since 4.0
 */
public final class SourceTypeWrapper<T extends Source> extends TypeWrapper implements SourceElement, ParameterizableTypeElement {

  private final Class<T> aClass;
  private final LazyValue<List<Type>> sourceGenerics;

  public SourceTypeWrapper(Class<T> aClass, ClassTypeLoader typeLoader) {
    super(aClass, typeLoader);
    this.aClass = aClass;
    this.sourceGenerics =
        new LazyValue<>(() -> IntrospectionUtils.getSuperClassGenerics(aClass, Source.class.isAssignableFrom(aClass)
            ? Source.class
            : org.mule.sdk.api.runtime.source.Source.class)
            .stream()
            .map(e -> new TypeWrapper(e, typeLoader))
            .collect(toList()));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getSuperClassGenerics() {
    return sourceGenerics.get();
  }

  @Override
  public Optional<MethodElement> getOnResponseMethod() {
    return getMethodAnnotatedWith(OnSuccess.class, org.mule.sdk.api.annotation.execution.OnSuccess.class);
  }

  @Override
  public Optional<MethodElement> getOnErrorMethod() {
    return getMethodAnnotatedWith(OnError.class, org.mule.sdk.api.annotation.execution.OnError.class);
  }

  @Override
  public Optional<MethodElement> getOnTerminateMethod() {
    return getMethodAnnotatedWith(OnTerminate.class, org.mule.sdk.api.annotation.execution.OnTerminate.class);
  }

  @Override
  public Optional<MethodElement> getOnBackPressureMethod() {
    return getMethodAnnotatedWith(OnBackPressure.class, org.mule.sdk.api.annotation.source.OnBackPressure.class);
  }

  private Optional<MethodElement> getMethodAnnotatedWith(Class<? extends Annotation>... annotationTypes) {
    Class<?> searchClass = aClass;
    Collection<Method> methods = null;
    while (!Object.class.equals(searchClass)) {
      methods = getMethodAnnotatedWith(searchClass, annotationTypes);
      if (methods.isEmpty()) {
        searchClass = searchClass.getSuperclass();
      } else {
        break;
      }
    }

    if (methods.isEmpty()) {
      return empty();
    } else if (methods.size() > 1) {
      throw new IllegalSourceModelDefinitionException(
                                                      format("Source declared in class '%s' declares more than one method annotated with '%s'",
                                                             aClass.getName(), annotationTypes[0].getSimpleName()));
    } else {
      return of(new MethodWrapper(methods.iterator().next(), typeLoader));
    }
  }

  private Collection<Method> getMethodAnnotatedWith(Class<?> searchClass, Class<? extends Annotation>... annotationTypes) {
    return Stream.of(annotationTypes).map(annotationType -> getMethodsAnnotatedWith(searchClass, annotationType, false))
        .flatMap(Collection::stream).collect(toList());
  }
}
