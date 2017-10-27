/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.runtime;

import static java.util.Optional.ofNullable;
import static org.springframework.core.ResolvableType.forMethodReturnType;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Wrapper for {@link Method} that provide utility methods to facilitate the introspection of a {@link Method}
 *
 * @since 4.0
 */
public final class MethodWrapper implements MethodElement {

  private final Method method;

  public MethodWrapper(Method method) {
    this.method = method;
  }

  /**
   * @return The wrapped method
   */
  @Override
  public Method getMethod() {
    return method;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class getDeclaringClass() {
    return method.getDeclaringClass();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameters() {
    final Parameter[] parameters = method.getParameters();
    List<ExtensionParameter> extensionParameters = new ArrayList<>(parameters.length);
    for (int i = 0; i < parameters.length; i++) {
      extensionParameters.add(new ParameterWrapper(method, i));
    }
    return extensionParameters;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameterGroups() {
    return getParametersAnnotatedWith(ParameterGroup.class);
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
        extensionParameters.add(new ParameterWrapper(method, i));
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
  public Annotation[] getAnnotations() {
    return method.getAnnotations();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(method.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class<?> getReturnType() {
    return forMethodReturnType(method).getRawClass();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof MethodElement && method.equals(((MethodElement) obj).getMethod());
  }

  @Override
  public int hashCode() {
    return method.hashCode();
  }
}
