/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.METHOD;

import org.mule.runtime.extension.api.annotation.Ignore;
import org.mule.runtime.module.extension.internal.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import java.util.List;

/**
 * {@link FunctionContainerElement} which works with the Java AST
 *
 * @since 4.1
 */
public class FunctionContainerASTElement extends ASTType implements FunctionContainerElement {

  private final TypeElement typeElement;
  private final ProcessingEnvironment processingEnvironment;

  /**
   * Creates a new {@link FunctionContainerASTElement}
   *
   * @param typeElement           The {@link TypeElement} representing the Java Class which contains DW Functions
   * @param processingEnvironment The AST {@link ProcessingEnvironment}
   */
  FunctionContainerASTElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    super(typeElement, processingEnvironment);
    this.typeElement = typeElement;
    this.processingEnvironment = processingEnvironment;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MethodElement> getFunctions() {
    return typeElement.getEnclosedElements()
        .stream()
        .filter(elem -> elem.getKind().equals(METHOD))
        .filter(elem -> elem.getAnnotation(Ignore.class) == null)
        .map(elem -> new MethodElementAST((ExecutableElement) elem, processingEnvironment))
        .collect(toList());
  }
}
