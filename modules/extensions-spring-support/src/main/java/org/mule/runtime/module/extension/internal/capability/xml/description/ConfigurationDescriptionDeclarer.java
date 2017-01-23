/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static java.util.Collections.emptyList;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.extension.api.annotation.connectivity.ConnectionProviders;

import java.util.List;
import java.util.Optional;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

/**
 * {@link AbstractDescriptionDeclarer} implementation that fills {@link ConfigurationDeclaration}s
 *
 * @since 4.0
 */
final class ConfigurationDescriptionDeclarer extends AbstractDescriptionDeclarer<ConfigurationDeclaration> {

  private final ParameterDescriptionDeclarer parameterDeclarer;
  private final OperationDescriptionDeclarer operationDeclarer;
  private final SourcesDescriptionDeclarer sourceDeclarer;

  ConfigurationDescriptionDeclarer(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    this.parameterDeclarer = new ParameterDescriptionDeclarer(processingEnv);
    this.operationDeclarer = new OperationDescriptionDeclarer(processingEnv);
    this.sourceDeclarer = new SourcesDescriptionDeclarer(processingEnv);
  }

  @Override
  void document(ConfigurationDeclaration declaration, TypeElement configElement) {
    declaration.setDescription(processor.getJavaDocSummary(processingEnv, configElement));
    operationDeclarer.document(declaration, configElement);
    sourceDeclarer.document(declaration, configElement);
    documentConnectionProviders(declaration, configElement);
    parameterDeclarer.document(declaration, configElement);
  }

  private void documentConnectionProviders(ConnectedDeclaration declaration, TypeElement element) {
    getConnectionProviderClasses(processingEnv, element)
        .forEach(providerElement -> findMatchingProvider(declaration, providerElement)
            .ifPresent(provider -> {
              provider.setDescription(processor.getJavaDocSummary(processingEnv, providerElement));
              parameterDeclarer.document(provider, providerElement);
            }));
  }

  private Optional<ConnectionProviderDeclaration> findMatchingProvider(ConnectedDeclaration<?> declaration, Element element) {
    return declaration.getConnectionProviders().stream()
        .filter(provider -> {
          String name = provider.getName().replace("-connection", "");
          String alias = getAliasValue(element);
          String defaultNaming = hyphenize(element.getSimpleName().toString().replace("Provider", ""));
          return name.equals(defaultNaming) || name.equals("connection") || name.equals(alias);
        })
        .findAny();
  }

  private List<TypeElement> getConnectionProviderClasses(ProcessingEnvironment processingEnv, TypeElement element) {
    ConnectionProviders providersAnnotation = processor.getAnnotationFromType(processingEnv, element, ConnectionProviders.class);
    if (providersAnnotation == null) {
      return emptyList();
    }
    return processor.getAnnotationClassesValue(element, ConnectionProviders.class, providersAnnotation.value());
  }
}
