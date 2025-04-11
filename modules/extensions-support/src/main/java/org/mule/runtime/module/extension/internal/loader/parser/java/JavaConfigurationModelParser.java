/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.api.util.MuleExtensionUtils.isAddAnnotationsToConfigClass;
import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceSingleAnnotation;
import static org.mule.runtime.module.extension.internal.loader.parser.java.ParameterDeclarationContext.forConfig;
import static org.mule.runtime.module.extension.internal.loader.parser.java.lib.JavaExternalLibModelParserUtils.parseExternalLibraryModels;
import static org.mule.runtime.module.extension.internal.loader.parser.java.stereotypes.JavaStereotypeModelParserUtils.resolveStereotype;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.getContainerAnnotationMinMuleVersion;
import static org.mule.runtime.module.extension.internal.loader.parser.java.utils.MinMuleVersionUtils.resolveConfigurationMinMuleVersion;

import static java.lang.String.format;
import static java.util.Collections.sort;
import static java.util.Collections.unmodifiableList;
import static java.util.Comparator.comparing;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang3.StringUtils.isBlank;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.deprecated.DeprecationModel;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.NoImplicit;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ConnectionProviderModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.FunctionModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.StereotypeModelFactory;
import org.mule.runtime.module.extension.internal.loader.parser.java.utils.ResolvedMinMuleVersion;
import org.mule.sdk.api.annotation.Configurations;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * {@link ConfigurationModelParser} for Java based syntax
 *
 * @since 4.5.0
 */
public class JavaConfigurationModelParser extends AbstractJavaModelParser implements ConfigurationModelParser {

  private final JavaExtensionModelParser extensionModelParser;
  private final ComponentElement configElement;

  public JavaConfigurationModelParser(JavaExtensionModelParser extensionModelParser,
                                      ExtensionElement extensionElement,
                                      ComponentElement configElement,
                                      ExtensionLoadingContext loadingContext) {
    super(extensionElement, loadingContext);
    this.extensionModelParser = extensionModelParser;
    this.configElement = configElement;

    parseStructure();
  }

  private void parseStructure() {
    checkConfigurationIsNotAnOperation(extensionElement, configElement);
    additionalModelProperties.add(new ImplementingTypeModelProperty(configElement.getDeclaringClass().orElse(Object.class)));
    additionalModelProperties.add(new ExtensionTypeDescriptorModelProperty(configElement));
  }

  @Override
  public String getName() {
    return mapReduceSingleAnnotation(configElement,
                                     "Configuration", configElement.getName(),
                                     Configuration.class,
                                     org.mule.sdk.api.annotation.Configuration.class,
                                     value -> value.getStringValue(Configuration::name),
                                     value -> value
                                         .getStringValue(org.mule.sdk.api.annotation.Configuration::name))
        .map(name -> isBlank(name) ? DEFAULT_CONFIG_NAME : name)
        .orElse(DEFAULT_CONFIG_NAME);
  }

  @Override
  public String getDescription() {
    return configElement.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupParsers() {
    return JavaExtensionModelParserUtils.getParameterGroupParsers(configElement.getParameters(),
                                                                  forConfig(configElement.getName(),
                                                                            loadingContext));
  }

  @Override
  public List<OperationModelParser> getOperationParsers() {
    List<OperationModelParser> operationModelParsers = new ArrayList<>(JavaExtensionModelParserUtils.getOperationParsers(
                                                                                                                         extensionModelParser,
                                                                                                                         extensionElement,
                                                                                                                         configElement,
                                                                                                                         loadingContext)
        .collect(toList()));
    sort(operationModelParsers, comparing(OperationModelParser::getName));
    return unmodifiableList(operationModelParsers);
  }

  @Override
  public List<SourceModelParser> getSourceModelParsers() {
    return JavaExtensionModelParserUtils.getSourceParsers(extensionElement, configElement.getSources(), loadingContext)
        .collect(toList());
  }

  @Override
  public List<ConnectionProviderModelParser> getConnectionProviderModelParsers() {
    return JavaExtensionModelParserUtils.getConnectionProviderModelParsers(
                                                                           extensionModelParser,
                                                                           extensionElement,
                                                                           configElement.getConnectionProviders(),
                                                                           loadingContext);
  }

  @Override
  public List<FunctionModelParser> getFunctionModelParsers() {
    return JavaExtensionModelParserUtils.getFunctionModelParsers(extensionElement,
                                                                 configElement.getFunctionContainers(),
                                                                 loadingContext);
  }

  @Override
  public ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty() {
    Class<?> extensionClass = extensionElement.getDeclaringClass().orElse(Object.class);
    Class<?> configClass = configElement.getDeclaringClass().orElse(Object.class);

    ClassLoader classLoader = extensionClass.getClassLoader() != null
        ? extensionClass.getClassLoader()
        : Thread.currentThread().getContextClassLoader();

    TypeAwareConfigurationFactory typeAwareConfigurationFactory =
        new TypeAwareConfigurationFactory(configClass, classLoader, isAddAnnotationsToConfigClass(loadingContext));

    return new ConfigurationFactoryModelProperty(typeAwareConfigurationFactory);
  }

  @Override
  public boolean isForceNoImplicit() {
    return configElement.isAnnotatedWith(NoImplicit.class) ||
        configElement.isAnnotatedWith(org.mule.sdk.api.annotation.NoImplicit.class);
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return parseExternalLibraryModels(configElement);
  }

  @Override
  public Optional<StereotypeModel> getStereotype(StereotypeModelFactory factory) {
    return resolveStereotype(configElement, "Configuration", getName(), factory);
  }

  private void checkConfigurationIsNotAnOperation(ExtensionElement extensionElement, ComponentElement componentElement) {
    List<OperationContainerElement> allOperations = new ArrayList<>();
    allOperations.addAll(extensionElement.getOperationContainers());
    allOperations.addAll(componentElement.getOperationContainers());

    for (OperationContainerElement operationClass : allOperations) {
      if (componentElement.isAssignableFrom(operationClass)
          || componentElement.isAssignableTo(operationClass)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                      componentElement.getName(), operationClass.getName()));
      }
    }
  }

  @Override
  public Optional<DeprecationModel> getDeprecationModel() {
    return JavaExtensionModelParserUtils.getDeprecationModel(configElement);
  }

  @Override
  public Optional<DisplayModel> getDisplayModel() {
    return JavaExtensionModelParserUtils.getDisplayModel(configElement, "configuration", configElement.getName());
  }

  @Override
  public Optional<ResolvedMinMuleVersion> getResolvedMinMuleVersion() {
    return of(resolveConfigurationMinMuleVersion(configElement,
                                                 getContainerAnnotationMinMuleVersion(extensionElement,
                                                                                      Configurations.class,
                                                                                      Configurations::value,
                                                                                      configElement)));
  }
}
