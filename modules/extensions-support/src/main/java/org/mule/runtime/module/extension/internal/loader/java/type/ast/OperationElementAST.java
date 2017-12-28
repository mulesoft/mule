/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * {@link OperationElement} implementation which works with the Java AST
 *
 * @since 4.1
 */
public class OperationElementAST extends MethodElementAST<OperationContainerElement> implements OperationElement {

  public OperationElementAST(ExecutableElement method, ProcessingEnvironment processingEnvironment) {
    super(method, processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public OperationContainerElement getEnclosingType() {
    return new OperationContainerElementAST((TypeElement) method.getEnclosingElement(), processingEnvironment);
  }
}
