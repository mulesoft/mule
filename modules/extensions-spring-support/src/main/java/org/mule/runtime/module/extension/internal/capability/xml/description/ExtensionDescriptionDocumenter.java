/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.capability.xml.description;

import static org.mule.runtime.core.api.util.StringUtils.EMPTY;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.util.NameUtils.hyphenize;
import static org.mule.runtime.extension.internal.loader.util.JavaParserUtils.getInfoFromAnnotation;

import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.extension.api.annotation.Configuration;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.TypeElement;

/**
 * {@link AbstractDescriptionDocumenter} implementation that picks a {@link ExtensionDeclaration} on which a
 * {@link ExtensionModel} has already been described.
 *
 * @since 4.0
 */
final class ExtensionDescriptionDocumenter extends AbstractDescriptionDocumenter {

  private final RoundEnvironment roundEnv;
  private final ConfigurationDescriptionDocumenter configDocumenter;
  private final OperationDescriptionDocumenter operationDocumenter;
  private final SourcesDescriptionDocumenter sourceDocumenter;

  ExtensionDescriptionDocumenter(ProcessingEnvironment processingEnvironment, RoundEnvironment roundEnvironment) {
    super(processingEnvironment);
    this.roundEnv = roundEnvironment;
    this.operationDocumenter = new OperationDescriptionDocumenter(processingEnv);
    this.sourceDocumenter = new SourcesDescriptionDocumenter(processingEnv);
    this.configDocumenter = new ConfigurationDescriptionDocumenter(processingEnvironment);
  }

  /**
   * Sets the description of the given {@link ExtensionDeclaration} and its inner configs and operations by extracting information
   * of the AST tree represented by {@code extensionElement} and {@code roundEnvironment}
   *
   * @param extensionDeclaration a {@link ExtensionDeclaration} on which configurations and operations have already been declared
   * @param extensionElement     a {@link TypeElement} generated by an annotation {@link Processor}
   */
  void document(ExtensionDeclaration extensionDeclaration, TypeElement extensionElement) {
    extensionDeclaration.setDescription(processor.getJavaDocSummary(processingEnv, extensionElement));
    sourceDocumenter.document(extensionElement, extensionDeclaration);
    operationDocumenter.document(extensionElement, extensionDeclaration);
    documentConfigurations(extensionDeclaration, extensionElement);
  }

  private void documentConfigurations(ExtensionDeclaration extensionDeclaration, TypeElement extensionElement) {
    Set<TypeElement> configurations = processor.getTypeElementsAnnotatedWith(Configuration.class, roundEnv);
    configurations.addAll(processor.getTypeElementsAnnotatedWith(org.mule.sdk.api.annotation.Configuration.class, roundEnv));
    if (!configurations.isEmpty()) {
      configurations
          .forEach(config -> findMatchingConfiguration(extensionDeclaration, config)
              .ifPresent(configDeclaration -> configDocumenter.document(extensionDeclaration, configDeclaration, config)));

      configDocumenter.documentConnectionProviders(extensionDeclaration, extensionElement);
    } else {
      configDocumenter.document(extensionDeclaration, extensionDeclaration.getConfigurations().get(0), extensionElement);
      extensionDeclaration.getConfigurations().get(0).setDescription(DEFAULT_CONFIG_DESCRIPTION);
    }
  }

  private Optional<ConfigurationDeclaration> findMatchingConfiguration(ExtensionDeclaration declaration, TypeElement element) {
    List<ConfigurationDeclaration> configurations = declaration.getConfigurations();
    if (configurations.size() == 1) {
      return Optional.of(configurations.get(0));
    }
    return configurations.stream()
        .filter(config -> {
          String annotationName = getInfoFromAnnotation(element,
                                                        Configuration.class,
                                                        org.mule.sdk.api.annotation.Configuration.class,
                                                        Configuration::name,
                                                        org.mule.sdk.api.annotation.Configuration::name)
                                                            .orElse(EMPTY);
          String name = config.getName();
          String defaultNaming = hyphenize(element.getSimpleName().toString());
          return name.equals(defaultNaming) || name.equals(annotationName) || name.equals(DEFAULT_CONFIG_NAME);
        })
        .findAny();
  }
}
