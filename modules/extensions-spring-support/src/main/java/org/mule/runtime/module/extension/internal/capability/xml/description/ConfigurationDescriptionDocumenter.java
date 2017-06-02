/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.DEFAULT_CONNECTION_PROVIDER_NAME;
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
 * {@link AbstractDescriptionDocumenter} implementation that fills {@link ConfigurationDeclaration}s
 *
 * @since 4.0
 */
final class ConfigurationDescriptionDocumenter extends AbstractDescriptionDocumenter<ConfigurationDeclaration> {

  private final ParameterDescriptionDocumenter parameterDeclarer;
  private final OperationDescriptionDocumenter operationDeclarer;
  private final SourcesDescriptionDocumenter sourceDeclarer;

  ConfigurationDescriptionDocumenter(ProcessingEnvironment processingEnv) {
    super(processingEnv);
    this.parameterDeclarer = new ParameterDescriptionDocumenter(processingEnv);
    this.operationDeclarer = new OperationDescriptionDocumenter(processingEnv);
    this.sourceDeclarer = new SourcesDescriptionDocumenter(processingEnv);
  }

  @Override
  void document(ConfigurationDeclaration declaration, TypeElement configElement) {
    declaration.setDescription(processor.getJavaDocSummary(processingEnv, configElement));
    operationDeclarer.document(declaration, configElement);
    sourceDeclarer.document(declaration, configElement);
    documentConnectionProviders(declaration, configElement);
    parameterDeclarer.document(declaration, configElement);
  }

  private void documentConnectionProviders(ConfigurationDeclaration declaration, TypeElement element) {
    getConnectionProviderClasses(processingEnv, element)
        .forEach(providerElement -> findMatchingProvider(declaration, providerElement)
            .ifPresent(provider -> {
              provider.setDescription(processor.getJavaDocSummary(processingEnv, providerElement));
              parameterDeclarer.document(provider, providerElement);
            }));
  }

  private Optional<ConnectionProviderDeclaration> findMatchingProvider(ConnectedDeclaration<?> declaration, Element element) {
    String alias = getAliasValue(element);
    String defaultNaming = hyphenize(element.getSimpleName().toString().replace("Provider", ""));
    return declaration.getConnectionProviders().stream()
        .filter(provider -> {
          String name = provider.getName();
          if (isNotBlank(alias)) {
            return name.equals(alias);
          } else {
            return name.equals(defaultNaming) || name.equals(DEFAULT_CONNECTION_PROVIDER_NAME);
          }
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
