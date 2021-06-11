/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.springframework.core.ResolvableType.forMethodReturnType;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.api.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;

import javax.lang.model.element.ExecutableElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Wrapper for {@link Method} that provide utility methods to facilitate the introspection of a {@link Method}
 *
 * @since 4.0
 */
public class MethodWrapper<T extends Type> implements MethodElement<T> {

  protected final Method method;
  protected ClassTypeLoader typeLoader;

  public MethodWrapper(Method method, ClassTypeLoader typeLoader) {
    this.method = method;
    this.typeLoader = typeLoader;
  }

  /**
   * @return The wrapped method
   */
  @Override
  public Optional<Method> getMethod() {
    return of(method);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getExceptionTypes() {
    return Stream.of(method.getExceptionTypes())
        .map(type -> new TypeWrapper(type, typeLoader))
        .collect(toList());
  }

  @Override
  public T getEnclosingType() {
    return (T) new TypeWrapper(getDeclaringClass().get(), typeLoader);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ExecutableElement> getElement() {
    return empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<Class<?>> getDeclaringClass() {
    return Optional.ofNullable(method.getDeclaringClass());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameters() {
    final Parameter[] parameters = method.getParameters();
    List<ExtensionParameter> extensionParameters = new ArrayList<>(parameters.length);
    for (int i = 0; i < parameters.length; i++) {
      extensionParameters.add(new ParameterWrapper(method, i, typeLoader));
    }
    return extensionParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameterGroups() {
    List<ExtensionParameter> extensionParametersGroups = new LinkedList<>(getParametersAnnotatedWith(ParameterGroup.class));
    extensionParametersGroups.addAll(getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.ParameterGroup.class));
    return extensionParametersGroups;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass) {
    List<ExtensionParameter> extensionParameters = new LinkedList<>();
    final Parameter[] parameters = method.getParameters();
    for (int i = 0; i < parameters.length; i++) {
      if (parameters[i].getAnnotation(annotationClass) != null) {
        extensionParameters.add(new ParameterWrapper(method, i, typeLoader));
      }
    }
    return extensionParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return method.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(method.getAnnotation(annotationClass));
  }

  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    return isAnnotatedWith(annotationClass)
        ? Optional.of(new ClassBasedAnnotationValueFetcher<>(annotationClass, method, typeLoader))
        : empty();
  }

  @Override
  public Type getReturnType() {
    return new TypeWrapper(forMethodReturnType(method), typeLoader);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof MethodElement && method.equals(((MethodElement) obj).getMethod().orElse(null));
  }

  @Override
  public int hashCode() {
    return method.hashCode();
  }
}
