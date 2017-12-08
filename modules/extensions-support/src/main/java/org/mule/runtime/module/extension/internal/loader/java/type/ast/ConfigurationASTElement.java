/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.Sources;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;
import org.mule.runtime.module.extension.internal.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ConnectionProviderElement;
import org.mule.runtime.module.extension.internal.loader.java.type.FunctionContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.type.SourceElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.List;

/**
 * {@link ConfigurationElement}
 *
 * @since 4.1
 */
public class ConfigurationASTElement extends ASTType implements ConfigurationElement {

  public ConfigurationASTElement(TypeElement typeElement, ProcessingEnvironment processingEnvironment) {
    super(typeElement, processingEnvironment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ConnectionProviderElement> getConnectionProviders() {
    return getValueFromAnnotation(ConnectionProviders.class).map(valueFetcher -> valueFetcher
        .getClassArrayValue(ConnectionProviders::value)
        .stream()
        .map(connElem -> (ConnectionProviderElement) new ConnectionProviderASTElement(((ASTType) connElem).getTypeElement(),
                                                                                      processingEnvironment))
        .collect(toList())).orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<FunctionContainerElement> getFunctionContainers() {
    return getValueFromAnnotation(ExpressionFunctions.class).map(valueFetcher -> valueFetcher
        .getClassArrayValue(ExpressionFunctions::value)
        .stream()
        .map(functionElem -> (FunctionContainerElement) new FunctionContainerASTElement(((ASTType) functionElem).getTypeElement(),
                                                                                        processingEnvironment))
        .collect(toList())).orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<SourceElement> getSources() {
    return getValueFromAnnotation(Sources.class).map(valueFetcher -> valueFetcher
        .getClassArrayValue(Sources::value)
        .stream()
        .map(sourceElem -> (SourceElement) new SourceElementAST(((ASTType) sourceElem).getTypeElement(), processingEnvironment))
        .collect(toList())).orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<OperationContainerElement> getOperationContainers() {
    return getValueFromAnnotation(Operations.class).map(valueFetcher -> valueFetcher
        .getClassArrayValue(Operations::value)
        .stream()
        .map(operationElem -> (OperationContainerElement) new OperationContainerElementAST(((ASTType) operationElem)
            .getTypeElement(),
                                                                                           processingEnvironment))
        .collect(toList())).orElse(emptyList());
  }
}
