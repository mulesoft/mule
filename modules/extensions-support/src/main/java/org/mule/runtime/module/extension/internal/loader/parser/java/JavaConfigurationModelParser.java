/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.parser.java;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.extension.api.annotation.Extension.DEFAULT_CONFIG_NAME;
import static org.mule.runtime.extension.api.util.NameUtils.getComponentDeclarationTypeName;
import static org.mule.runtime.module.extension.internal.loader.java.MuleExtensionAnnotationParser.parseLayoutAnnotations;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionDefinitionParserUtils.isParameterGroup;
import static org.mule.runtime.module.extension.internal.loader.parser.java.JavaExtensionDefinitionParserUtils.parseExternalLibraryModels;
import static org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils.isProcessorChain;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getFieldsWithGetters;

import org.mule.runtime.api.meta.model.ExternalLibraryModel;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.api.meta.model.declaration.fluent.Declarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasNestedComponentsDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.HasParametersDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.NamedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclarer;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclarer;
import org.mule.runtime.api.meta.model.display.DisplayModel;
import org.mule.runtime.api.meta.model.display.LayoutModel;
import org.mule.runtime.extension.api.annotation.Configuration;
import org.mule.runtime.extension.api.annotation.NoImplicit;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.ExclusiveOptionals;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.ParameterGroup;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.exception.IllegalConfigurationModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.module.extension.api.loader.java.type.ComponentElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionElement;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.api.loader.java.type.OperationContainerElement;
import org.mule.runtime.module.extension.api.loader.java.type.Type;
import org.mule.runtime.module.extension.api.loader.java.type.WithAlias;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.TypeAwareConfigurationFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.ConfigurationFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionTypeDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.parser.ConfigurationModelParser;
import org.mule.runtime.module.extension.internal.loader.parser.ParameterGroupModelParser;
import org.mule.runtime.module.extension.internal.loader.utils.ModelLoaderUtils;
import org.mule.runtime.module.extension.internal.loader.utils.ParameterDeclarationContext;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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
        List<ParameterDeclarer> groupParams = declaredAsGroup(component, declarationContext, extensionParameter);
        declarerList.addAll(groupParams);
        continue;
      }

    }
  }

  private List<ParameterDeclarer> declaredAsGroup(HasParametersDeclarer component,
//                                                  ParameterDeclarationContext declarationContext,
                                                  ExtensionParameter groupParameter)
      throws IllegalParameterModelDefinitionException {

    ParameterGroup groupAnnotation = groupParameter.getAnnotation(ParameterGroup.class).get();

    final String groupName = groupAnnotation.name();
    // TODO: This validation goes into the main loader
//    if (DEFAULT_GROUP_NAME.equals(groupName)) {
//      throw new IllegalParameterModelDefinitionException(
//          format("%s '%s' defines parameter group of name '%s' which is the default one. "
//                  + "@%s cannot be used with the default group name",
//              getComponentDeclarationTypeName(((Declarer) component)
//                  .getDeclaration()),
//              ((NamedDeclaration) ((Declarer) component).getDeclaration())
//                  .getName(),
//              groupName,
//              ParameterGroup.class.getSimpleName()));
//    }






    //TODO: This validation also goes into the loader
//    ParameterGroupDeclarer declarer = component.onParameterGroup(groupName);
//    if (declarer.getDeclaration().getModelProperty(ParameterGroupModelProperty.class).isPresent()) {
//      throw new IllegalParameterModelDefinitionException(format("Parameter group '%s' has already been declared on %s '%s'",
//          groupName,
//          getComponentDeclarationTypeName(((Declarer) component)
//              .getDeclaration()),
//          ((NamedDeclaration) ((Declarer) component).getDeclaration())
//              .getName()));
//    } else {
      declarer.withModelProperty(new ParameterGroupModelProperty(
          new ParameterGroupDescriptor(groupName, type,
              groupParameter.getType()
                  .asMetadataType(),
              // TODO: Eliminate dependency to
              // Annotated Elements
              groupParameter.getDeclaringElement()
                  .orElse(null),
              groupParameter)));
//    }

    final List<FieldElement> annotatedParameters = type.getAnnotatedFields(Parameter.class);
    type.getAnnotation(ExclusiveOptionals.class).ifPresent(annotation -> {
      Set<String> optionalParamNames = annotatedParameters.stream()
          .filter(f -> !f.isRequired())
          .map(WithAlias::getAlias)
          .collect(toSet());

      declarer.withExclusiveOptionals(optionalParamNames, annotation.isOneRequired());
    });

    declarer.withDslInlineRepresentation(groupAnnotation.showInDsl());

    groupParameter.getAnnotation(DisplayName.class)
        .ifPresent(displayName -> declarer.withDisplayModel(DisplayModel.builder().displayName(displayName.value()).build()));

    parseLayoutAnnotations(groupParameter, LayoutModel.builder()).ifPresent(declarer::withLayout);

    declarer.withModelProperty(new ExtensionParameterDescriptorModelProperty(groupParameter));

    if (!annotatedParameters.isEmpty()) {
      return declare(component, annotatedParameters, declarationContext, declarer);
    } else {
      return declare(component, getFieldsWithGetters(type), declarationContext, declarer);
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
