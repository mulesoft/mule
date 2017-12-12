/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.List;

/**
 * {@link OperationContainerElement} implementation which works with the Java AST.
 *
 * @since 4.1
 */
public class OperationContainerElementAST extends ASTType implements OperationContainerElement {

  /**
   *
   * @param typeElement
   * @param processingEnvironment
   */
  OperationContainerElementAST(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    super(typeElement, processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MethodElement> getOperations() {
    return getMethods().stream()
        .filter(elem -> !elem.isAnnotatedWith(Ignore.class))
        .collect(toList());
  }
}
