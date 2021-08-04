/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionModelParserUtils.parseExternalLibraryModels;

import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
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
import org.mule.runtime.module.extension.internal.loader.parser.OperationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.SourceModelParser;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JavaConfigurationModelParser implements ConfigurationModelParser {

  private final ExtensionElement extensionElement;
  private final ComponentElement configElement;
  private final ClassTypeLoader typeLoader;
  private final ExtensionLoadingContext loadingContext;

  public JavaConfigurationModelParser(ExtensionElement extensionElement,
                                      ComponentElement configElement,
                                      ClassTypeLoader typeLoader,
                                      ExtensionLoadingContext loadingContext) {
    checkConfigurationIsNotAnOperation(extensionElement, configElement);

    this.extensionElement = extensionElement;
    this.configElement = configElement;
    this.typeLoader = typeLoader;
    this.loadingContext = loadingContext;
  }

  @Override
  public String getName() {
    return configElement.getAnnotation(Configuration.class)
        .map(configuration -> isBlank(configuration.name()) ? DEFAULT_CONFIG_NAME : configuration.name())
        .orElse(DEFAULT_CONFIG_NAME);
  }

  @Override
  public String getDescription() {
    return configElement.getDescription();
  }

  @Override
  public List<ParameterGroupModelParser> getParameterGroupParsers() {
    return JavaExtensionModelParserUtils.getParameterGroupParsers(configElement.getParameters(), typeLoader);
  }

  @Override
  public List<OperationModelParser> getOperationParsers() {
    return JavaExtensionModelParserUtils.getOperationParsers(extensionElement, configElement, typeLoader, loadingContext);
  }

  @Override
  public List<SourceModelParser> getSourceModelParsers() {
    return JavaExtensionModelParserUtils.getSourceParsers(configElement.getSources(), typeLoader, loadingContext);
  }

  @Override
  public ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty() {
    Class<?> extensionClass = extensionElement.getDeclaringClass().orElse(Object.class);
    Class<?> configClass = configElement.getDeclaringClass().orElse(Object.class);

    ClassLoader classLoader = extensionClass.getClassLoader() != null
        ? extensionClass.getClassLoader()
        : Thread.currentThread().getContextClassLoader();

    TypeAwareConfigurationFactory typeAwareConfigurationFactory =
        new TypeAwareConfigurationFactory(configClass, classLoader);

    return new ConfigurationFactoryModelProperty(typeAwareConfigurationFactory);
  }

  @Override
  public boolean isForceNoExplicit() {
    return configElement.isAnnotatedWith(NoImplicit.class);
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return parseExternalLibraryModels(configElement);
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
  public List<ModelProperty> getAdditionalModelProperties() {
    List<ModelProperty> properties = new LinkedList<>();
    properties.add(new ImplementingTypeModelProperty(configElement.getDeclaringClass().orElse(Object.class)));
    properties.add(new ExtensionTypeDescriptorModelProperty(configElement));

    return unmodifiableList(properties);
  }
}
