/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.execution.OnError;
import org.mule.runtime.extension.api.annotation.execution.OnSuccess;
import org.mule.runtime.extension.api.annotation.execution.OnTerminate;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.SourceElement;
import org.mule.runtime.module.extension.internal.loader.java.type.Type;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.List;
import java.util.Optional;

/**
 * {@link SourceElement} implementation which works with the Java AST.
 *
 * @since 4.1
 */
class SourceElementAST extends ASTType implements SourceElement {

  SourceElementAST(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    super(typeElement, processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Type> getSuperClassGenerics() {
    return IntrospectionUtils.getSuperClassGenerics(this.typeElement, Source.class, processingEnvironment)
        .stream()
        .map(type -> new ASTType(type, processingEnvironment))
        .collect(toList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MethodElement> getOnResponseMethod() {
    return getMethods().stream()
        .filter(op -> op.isAnnotatedWith(OnSuccess.class))
        .findFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MethodElement> getOnErrorMethod() {
    return getMethods().stream()
        .filter(op -> op.isAnnotatedWith(OnError.class))
        .findFirst();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MethodElement> getOnTerminateMethod() {
    return getMethods().stream()
        .filter(op -> op.isAnnotatedWith(OnTerminate.class))
        .findFirst();
  }
}
