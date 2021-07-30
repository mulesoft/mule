/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionDefinitionParserUtils.parseExternalLibraryModels;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.NoImplicit;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ExtensionConfigurationDefinitionParser;

import java.util.ArrayList;
import java.util.List;

public class JavaExtensionConfigurationDefinitionParser implements ExtensionConfigurationDefinitionParser {

  private final ExtensionElement extensionElement;
  private final ComponentElement configurationElement;

  public JavaExtensionConfigurationDefinitionParser(ExtensionElement extensionElement, ComponentElement configurationElement) {
    this.extensionElement = extensionElement;
    this.configurationElement = configurationElement;
    checkConfigurationIsNotAnOperation(extensionElement, configurationElement);
  }

  @Override
  public String getName() {
    return configurationElement.getAnnotation(Configuration.class)
        .map(configuration -> isBlank(configuration.name()) ? DEFAULT_CONFIG_NAME : configuration.name())
        .orElse(DEFAULT_CONFIG_NAME);
  }

  @Override
  public String getDescription() {
    return configurationElement.getDescription();
  }

  @Override
  public ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty() {
    Class<?> extensionClass = extensionElement.getDeclaringClass().orElse(Object.class);
    Class<?> configClass = configurationElement.getDeclaringClass().orElse(Object.class);

    ClassLoader classLoader = extensionClass.getClassLoader() != null ? extensionClass.getClassLoader()
        : Thread.currentThread().getContextClassLoader();

    TypeAwareConfigurationFactory typeAwareConfigurationFactory =
        new TypeAwareConfigurationFactory(configClass, classLoader);

    return new ConfigurationFactoryModelProperty(typeAwareConfigurationFactory);
  }

  @Override
  public boolean isForceNoExplicit() {
    return configurationElement.isAnnotatedWith(NoImplicit.class);
  }

  @Override
  public List<ExternalLibraryModel> getExternalLibraryModels() {
    return parseExternalLibraryModels(configurationElement);
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
}
