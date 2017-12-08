/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.type.ast;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.meta.Category;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.annotation.Configurations;
import org.mule.runtime.extension.api.annotation.ExpressionFunctions;
import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.module.extension.internal.loader.java.type.AnnotationValueFetcher;
import org.mule.runtime.module.extension.internal.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.java.type.MethodElement;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;

import java.util.Collection;
import java.util.List;

/**
 * {@link ExtensionElement} implementation which works with the Java AST.
 *
 * @since 4.1
 */
public class ExtensionTypeElement extends ConfigurationASTElement implements ExtensionElement {

  private LazyValue<AnnotationValueFetcher<Extension>> extensionAnnotation =
      new LazyValue<>(() -> getValueFromAnnotation(Extension.class).get());

  /**
   *
   * @param typeElement
   * @param processingEnv
   */
  public ExtensionTypeElement(TypeElement typeElement, ProcessingEnvironment processingEnv) {
    super(typeElement, processingEnv);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<ConfigurationElement> getConfigurations() {
    return getValueFromAnnotation(Configurations.class).map(valFetcher -> valFetcher
        .getClassArrayValue(Configurations::value)
        .stream()
        .map(configType -> (ConfigurationElement) new ConfigurationASTElement(((ASTType) configType).getTypeElement(),
                                                                              processingEnvironment))
        .collect(toList()))
        .orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MethodElement> getFunctions() {
    return getValueFromAnnotation(ExpressionFunctions.class).map(valFetcher -> valFetcher
        .<ASTType>getClassArrayValue(ExpressionFunctions::value)
        .stream()
        .map(conWrapper -> new FunctionContainerASTElement(((ASTType) conWrapper).getTypeElement(), processingEnvironment))
        .map(FunctionContainerASTElement::getFunctions)
        .flatMap(Collection::stream)
        .collect(toList()))
        .orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<MethodElement> getOperations() {
    return getValueFromAnnotation(Operations.class).map(valFetcher -> valFetcher
        .<ASTType>getClassArrayValue(Operations::value)
        .stream()
        .map(conWrapper -> new OperationContainerElementAST(((ASTType) conWrapper).getTypeElement(), processingEnvironment))
        .map(OperationContainerElementAST::getOperations)
        .flatMap(Collection::stream)
        .collect(toList()))
        .orElse(emptyList());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Category getCategory() {
    return extensionAnnotation.get().getEnumValue(Extension::category);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getVendor() {
    return extensionAnnotation.get().getStringValue(Extension::vendor);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return extensionAnnotation.get().getStringValue(Extension::name);
  }
}
