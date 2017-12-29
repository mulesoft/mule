/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import org.mule.runtime.module.extension.internal.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.FunctionElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * /**
 * {@link FunctionElement} implementation which works with the Java AST
 *
 * @since 4.1
 */
public class FunctionElementAST extends MethodElementAST<FunctionContainerElement> implements FunctionElement {

  FunctionElementAST(ExecutableElement method, ProcessingEnvironment processingEnvironment) {
    super(method, processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public FunctionContainerElement getEnclosingType() {
    return new FunctionContainerASTElement((TypeElement) method.getEnclosingElement(), processingEnvironment);
  }
}
