/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Optional;

/**
 * Wrapper of {@link VariableElement} representing {@link ExtensionParameter}.
 *
 * @since 4.1
 */
public abstract class VariableElementAST implements ExtensionParameter {

  private final VariableElement param;
  private final ProcessingEnvironment processingEnvironment;
  private final ASTUtils astUtils;

  VariableElementAST(VariableElement param, ProcessingEnvironment processingEnvironment) {
    this.param = param;
    this.processingEnvironment = processingEnvironment;
    this.astUtils = new ASTUtils(processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Type getJavaType() {
    return astUtils.getReflectType(getType());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return param.getSimpleName().toString();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<A> getAnnotation(Class<A> annotationClass) {
    return ofNullable(param.getAnnotation(annotationClass));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <A extends Annotation> Optional<AnnotationValueFetcher<A>> getValueFromAnnotation(Class<A> annotationClass) {
    if (this.isAnnotatedWith(annotationClass)) {
      return of(astUtils.fromAnnotation(annotationClass, param));
    } else {
      return empty();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ASTType getType() {
    return new ASTType(param.asType(), processingEnvironment);
  }
}
