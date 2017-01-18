/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java;

import static java.lang.String.format;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_DESCRIPTION;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclarer;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ConfigurationElement;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.util.List;
import java.util.Optional;

/**
 * Helper class for declaring configurations through a {@link JavaModelLoaderDelegate}
 * @since 4.0
 */
final class ConfigModelLoaderDelegate extends AbstractModelLoaderDelegate {

  private static final String CONFIGURATION = "Configuration";

  ConfigModelLoaderDelegate(JavaModelLoaderDelegate delegate) {
    super(delegate);
  }

  void declareConfigurations(ExtensionDeclarer declaration, ExtensionElement extensionElement) {
    List<ConfigurationElement> configurations = extensionElement.getConfigurations();
    if (configurations.isEmpty()) {
      declareConfiguration(declaration, extensionElement, extensionElement);
    } else {
      for (ConfigurationElement configuration : configurations) {
        declareConfiguration(declaration, extensionElement, configuration);
      }
    }
  }

  private void declareConfiguration(ExtensionDeclarer declarer, ExtensionElement extensionType,
                                    ComponentElement configurationType) {
    checkConfigurationIsNotAnOperation(configurationType.getDeclaringClass());
    ConfigurationDeclarer configurationDeclarer;

    Optional<Configuration> configurationAnnotation = configurationType.getAnnotation(Configuration.class);
    if (configurationAnnotation.isPresent()) {
      final Configuration configuration = configurationAnnotation.get();
      configurationDeclarer = declarer.withConfig(configuration.name()).describedAs(configuration.description());
    } else {
      configurationDeclarer =
          declarer.withConfig(DEFAULT_CONFIG_NAME).describedAs(DEFAULT_CONFIG_DESCRIPTION);
    }

    configurationDeclarer.withModelProperty(
                                            new ConfigurationFactoryModelProperty(new TypeAwareConfigurationFactory(configurationType
                                                .getDeclaringClass(),
                                                                                                                    extensionType
                                                                                                                        .getDeclaringClass()
                                                                                                                        .getClassLoader())))
        .withModelProperty(new ImplementingTypeModelProperty(configurationType.getDeclaringClass()));

    loader.parseExternalLibs(configurationType, configurationDeclarer);
    loader.declareFieldBasedParameters(configurationDeclarer, configurationType.getParameters(),
                                       new ParameterDeclarationContext(CONFIGURATION, configurationDeclarer.getDeclaration()));

    getOperationLoaderDelegate().declareOperations(declarer, configurationDeclarer, configurationType);
    getSourceModelLoaderDelegate().declareMessageSources(declarer, configurationDeclarer, configurationType);
    getConnectionProviderModelLoaderDelegate().declareConnectionProviders(configurationDeclarer, configurationType);
  }

  private void checkConfigurationIsNotAnOperation(Class<?> configurationType) {
    Class<?>[] operationClasses = loader.getOperationClasses(getExtensionType());
    for (Class<?> operationClass : operationClasses) {
      if (configurationType.isAssignableFrom(operationClass) || operationClass.isAssignableFrom(configurationType)) {
        throw new IllegalConfigurationModelDefinitionException(
                                                               format("Configuration class '%s' cannot be the same class (nor a derivative) of any operation class '%s",
                                                                      configurationType.getName(), operationClass.getName()));
      }
    }
  }
}
