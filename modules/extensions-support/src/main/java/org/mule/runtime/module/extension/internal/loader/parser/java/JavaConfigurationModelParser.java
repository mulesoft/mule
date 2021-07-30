/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionDefinitionParserUtils.isParameterGroup;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionDefinitionParserUtils.parseExternalLibraryModels;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.NoImplicit;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class JavaConfigurationModelParser implements ConfigurationModelParser {

  private final ExtensionElement extensionElement;
  private final ComponentElement configElement;

  public JavaConfigurationModelParser(ExtensionElement extensionElement, ComponentElement configElement) {
    this.extensionElement = extensionElement;
    this.configElement = configElement;
    checkConfigurationIsNotAnOperation(extensionElement, configElement);
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
    List<ExtensionParameter> parameters = configElement.getParameters();
    checkAnnotationsNotUsedMoreThanOnce(parameters, Connection.class, Config.class, MetadataKeyId.class);

    List<ParameterGroupModelParser> groups = new LinkedList<>();
    for (ExtensionParameter extensionParameter : parameters) {
      if (!extensionParameter.shouldBeAdvertised()) {
        continue;
      }

      if (isParameterGroup(extensionParameter)) {
        groups.add(new JavaDeclaredParameterGroupModelParser(extensionParameter));
        continue;
      }

    }

    return unmodifiableList(groups);
  }

  @Override
  public ConfigurationFactoryModelProperty getConfigurationFactoryModelProperty() {
    Class<?> extensionClass = extensionElement.getDeclaringClass().orElse(Object.class);
    Class<?> configClass = configElement.getDeclaringClass().orElse(Object.class);

    ClassLoader classLoader = extensionClass.getClassLoader() != null ? extensionClass.getClassLoader()
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

  private void checkAnnotationsNotUsedMoreThanOnce(List<? extends ExtensionParameter> parameters,
                                                   Class<? extends Annotation>... annotations) {
    for (Class<? extends Annotation> annotation : annotations) {
      final long count = parameters.stream().filter(param -> param.isAnnotatedWith(annotation)).count();
      if (count > 1) {
        throw new IllegalModelDefinitionException(
            format("The defined parameters %s from %s, uses the annotation @%s more than once",
                parameters.stream().map(p -> p.getName()).collect(toList()),
                parameters.iterator().next().getOwnerDescription(),
                annotation.getSimpleName()));
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
