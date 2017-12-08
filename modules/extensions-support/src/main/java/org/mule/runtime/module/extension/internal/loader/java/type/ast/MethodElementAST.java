/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * {@link MethodElement} implementation which works with the Java AST
 *
 * @since 4.1.0
 */
public class MethodElementAST implements MethodElement {

  private final ExecutableElement method;
  private final ProcessingEnvironment processingEnvironment;
  private final LazyValue<List<ExtensionParameter>> parameters;
  private final LazyValue<List<ExtensionParameter>> parameterGroups;
  private final ASTUtils astUtils;

  MethodElementAST(ExecutableElement method, ProcessingEnvironment processingEnvironment) {
    this.method = method;
    this.processingEnvironment = processingEnvironment;
    this.astUtils = new ASTUtils(processingEnvironment);

    parameters = new LazyValue<>(() -> method.getParameters().stream()
        .map(param -> new MethodParameterElementAST(param, processingEnvironment))
        .collect(toList()));
    parameterGroups = new LazyValue<>(() -> getParametersAnnotatedWith(ParameterGroup.class));
  }

  /**
   * @return {@link Optional#empty()}. This implementation doesn't support to return {@link Method}.
   */
  @Override
  public Optional<Method> getMethod() {
    return empty();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationContainerElement getEnclosingType() {
    return new OperationContainerElementAST((TypeElement) method.getEnclosingElement(), processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameters() {
    return parameters.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParameterGroups() {
    return parameterGroups.get();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ExtensionParameter> getParametersAnnotatedWith(Class<? extends Annotation> annotationClass) {
    return getParameters().stream()
        .filter(param -> param.getAnnotation(annotationClass).isPresent())
        .collect(toList());
  }

  /**
  * {@inheritDoc}
  */
  @Override
  public ASTType getReturnType() {
    return new ASTType(method.getReturnType(), processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return method.getSimpleName().toString();
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
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    return Optional.ofNullable(astUtils.fromAnnotation(annotationClass, method));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Class getDeclaringClass() {
    try {
      return Class
          .forName(processingEnvironment.getElementUtils().getBinaryName((TypeElement) method.getEnclosingElement()).toString());
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;

    if (o == null || getClass() != o.getClass())
      return false;

    MethodElementAST that = (MethodElementAST) o;

    return new EqualsBuilder()
        .append(method, that.method)
        .isEquals();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int hashCode() {
    return new HashCodeBuilder(17, 37)
        .append(method)
        .toHashCode();
  }
}
