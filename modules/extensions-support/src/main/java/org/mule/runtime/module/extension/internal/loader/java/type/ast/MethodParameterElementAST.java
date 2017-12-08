/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Optional.empty;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.VariableElement;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * @since 4.1
 */
public class MethodParameterElementAST extends VariableElementAST {

  MethodParameterElementAST(VariableElement param, ProcessingEnvironment processingEnvironment) {
    super(param, processingEnvironment);
  }

  @Override
  public String getOwnerDescription() {
    return "Method";
  }

  @Override
  public Optional<AnnotatedElement> getDeclaringElement() {
    return empty();
  }
}
