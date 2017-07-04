/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.Thread.currentThread;
import static java.util.Optional.empty;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getRealName;
import org.mule.metadata.api.ClassTypeLoader;
import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.parameter.ValuesProviderModel;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.values.ValuePart;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.ExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.values.ValuesProvider;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValuesProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValuesProviderFactoryModelProperty.ValuesProviderFactoryModelPropertyBuilder;
import org.mule.runtime.module.extension.internal.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.internal.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterizableTypeWrapper;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@link DeclarationEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for Source,
 * Operation, Configuration and Connection Provider Parameters and ParameterGroups annotated with {@link OfValues}.
 * If a parameter or parameter group is annotated, this one will have a related {@link ValuesProvider}
 *
 * @since 4.0
 */
public class ValueProvidersParameterDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  private ClassTypeLoader typeLoader;

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    Optional<ImplementingTypeModelProperty> implementingType =
        extractImplementingTypeProperty(extensionLoadingContext.getExtensionDeclarer().getDeclaration());

    typeLoader = ExtensionsTypeLoaderFactory.getDefault().createTypeLoader(currentThread().getContextClassLoader());

    if (implementingType.isPresent()) {
      new IdempotentDeclarationWalker() {

        @Override
        public void onSource(SourceDeclaration declaration) {
          enrichContainerModel(declaration);
        }

        @Override
        public void onOperation(OperationDeclaration declaration) {
          enrichContainerModel(declaration);
        }

        @Override
        protected void onConfiguration(ConfigurationDeclaration declaration) {
          enrichContainerModel(declaration);
        }

        @Override
        protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
          enrichContainerModel(declaration);
        }
      }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
    }

    typeLoader = null;
  }

  /**
   * The method will look for parameters of the given {@link ParameterizedDeclaration declaration} and is a parameter or
   * parameter group annotated with {@link OfValues} if found, a {@link ValuesProviderModel} will be added to this element
   * to communicate that values can be provided.
   * <p>
   * Also the {@link ParameterDeclaration parameters} of the {@link ParameterizedDeclaration declaration} will be
   * enriched.
   *
   * @param containerDeclaration declaration to introspect their parameters
   */
  private void enrichContainerModel(ParameterizedDeclaration<?> containerDeclaration) {
    List<ParameterDeclaration> allParameters = containerDeclaration.getAllParameters();

    Map<String, String> parameterNames = getContainerParameterNames(allParameters);
    Map<ParameterGroupDeclaration, Class<? extends ValuesProvider>> dynamicGroupOptions =
        introspectParameterGroups(containerDeclaration.getParameterGroups());
    Map<ParameterDeclaration, Class<? extends ValuesProvider>> dynamicOptions = introspectParameters(allParameters);

    dynamicOptions.forEach((paramDeclaration, resolverClass) -> enrichParameter(resolverClass,
                                                                                paramDeclaration,
                                                                                paramDeclaration::setValuesProviderModel,
                                                                                1,
                                                                                parameterNames, paramDeclaration.getName()));

    dynamicGroupOptions
        .forEach((paramGroupDeclaration, resolverClass) -> getParts(paramGroupDeclaration)
            .forEach((paramDeclaration, order) -> enrichParameter(resolverClass, paramDeclaration,
                                                                  paramDeclaration::setValuesProviderModel, order, parameterNames,
                                                                  paramGroupDeclaration.getName())));
  }

  /**
   * Enriches a parameter that has an associated {@link ValuesProvider}
   *
   * @param resolverClass           the class of the {@link ValuesProvider}
   * @param paramDeclaration        {@link ParameterDeclaration} or {@link ParameterGroupDeclaration} paramDeclaration
   * @param containerParameterNames parameters container's names
   */
  private void enrichParameter(Class<? extends ValuesProvider> resolverClass,
                               BaseDeclaration paramDeclaration,
                               Consumer<ValuesProviderModel> valueProviderModelConsumer, Integer partOrder,
                               Map<String, String> containerParameterNames, String name) {

    ValuesProviderFactoryModelPropertyBuilder propertyBuilder =
        ValuesProviderFactoryModelProperty.builder(resolverClass);
    ParameterizableTypeWrapper resolverClassWrapper = new ParameterizableTypeWrapper(resolverClass);
    List<ExtensionParameter> resolverParameters = resolverClassWrapper.getParametersAnnotatedWith(Parameter.class);

    resolverParameters.forEach(param -> propertyBuilder
        .withInjectableParameter(param.getName(), param.getMetadataType(typeLoader), param.isRequired()));

    enrichWithConnection(propertyBuilder, resolverClassWrapper);
    enrichWithConfiguration(propertyBuilder, resolverClassWrapper);

    paramDeclaration.addModelProperty(propertyBuilder.build());

    valueProviderModelConsumer
        .accept(new ValuesProviderModel(getRequiredParametersAliases(resolverParameters, containerParameterNames), partOrder,
                                        name));
  }

  /**
   * Introspects the given {@link ParameterizableTypeWrapper parameterizableComponent} looking if this ones uses a
   * {@link Connection}, if this is true this method will indicate to the {@link ValuesProviderFactoryModelPropertyBuilder}
   * that the correspondent {@link ValuesProvider} will require a connection.
   *
   * @param modelPropertyBuilder     Options Resolver Model Property Builder
   * @param parameterizableComponent component to introspect
   */
  private void enrichWithConnection(ValuesProviderFactoryModelPropertyBuilder modelPropertyBuilder,
                                    ParameterizableTypeWrapper parameterizableComponent) {
    List<FieldElement> connectionFields = parameterizableComponent.getAnnotatedFields(Connection.class);
    if (!connectionFields.isEmpty()) {
      FieldElement fieldElement = connectionFields.get(0);
      modelPropertyBuilder.withConnection(fieldElement.getField());
    }
  }

  /**
   * Introspects the given {@link ParameterizableTypeWrapper parameterizableComponent} looking if this ones uses a
   * {@link Config}, if this is true this method will indicate to the {@link ValuesProviderFactoryModelPropertyBuilder}
   * that the correspondent {@link ValuesProvider} will require a config.
   *
   * @param modelPropertyBuilder     Options Resolver Model Property Builder
   * @param parameterizableComponent component to introspect
   */
  private void enrichWithConfiguration(ValuesProviderFactoryModelPropertyBuilder modelPropertyBuilder,
                                       ParameterizableTypeWrapper parameterizableComponent) {
    List<FieldElement> configFields = parameterizableComponent.getAnnotatedFields(Config.class);
    if (!configFields.isEmpty()) {
      FieldElement fieldElement = configFields.get(0);
      modelPropertyBuilder.withConfig(fieldElement.getField());
    }
  }

  /**
   * Given a list of {@link ParameterDeclaration}, introspect it and finds all the considered parameters with an associated
   * {@link ValuesProvider}
   *
   * @param parameters parameters to introspect
   * @return a Map containing all the {@link ParameterDeclaration} with their correspondent {@link ValuesProvider}
   */
  private Map<ParameterDeclaration, Class<? extends ValuesProvider>> introspectParameters(List<ParameterDeclaration> parameters) {

    Map<ParameterDeclaration, Class<? extends ValuesProvider>> optionResolverEnabledParameters = new HashMap<>();

    parameters.forEach(param -> getAnnotation(param, OfValues.class)
        .ifPresent(optionAnnotation -> optionResolverEnabledParameters.put(param, optionAnnotation.value())));

    return optionResolverEnabledParameters;
  }

  /**
   * Given a list of {@link ParameterGroupDeclaration}, introspect it and finds all the considered parameters with an associated
   * {@link ValuesProvider}
   *
   * @param parameterGroups parameter groups to introspect
   * @return a Map containing all the {@link ParameterGroupDeclaration} with their correspondent {@link ValuesProvider}
   */
  private Map<ParameterGroupDeclaration, Class<? extends ValuesProvider>> introspectParameterGroups(List<ParameterGroupDeclaration> parameterGroups) {
    Map<ParameterGroupDeclaration, Class<? extends ValuesProvider>> optionResolverEnabledParameters = new HashMap<>();

    parameterGroups
        .forEach(paramGroup -> paramGroup.getModelProperty(ParameterGroupModelProperty.class)
            .ifPresent(modelProperty -> {

              AnnotatedElement container = modelProperty.getDescriptor().getContainer();
              if (container != null) {
                OfValues annotation = container.getAnnotation(OfValues.class);
                if (annotation != null) {
                  optionResolverEnabledParameters.put(paramGroup, annotation.value());
                }
              }
            }));

    return optionResolverEnabledParameters;
  }

  private <T extends Annotation> Optional<T> getAnnotation(ParameterDeclaration param, Class<T> annotationClass) {
    return getAnnotatedElement(param)
        .map(annotatedElement -> ofNullable(annotatedElement.getAnnotation(annotationClass)))
        .orElse(empty());
  }

  private List<String> getRequiredParametersAliases(List<ExtensionParameter> parameterDeclarations,
                                                    Map<String, String> parameterNames) {
    return parameterDeclarations.stream()
        .filter(ExtensionParameter::isRequired)
        .map(param -> parameterNames.getOrDefault(param.getName(), param.getName()))
        .collect(toList());
  }

  private Map<ParameterDeclaration, Integer> getParts(ParameterGroupDeclaration paramDeclaration) {
    Map<ParameterDeclaration, Integer> parts = new HashMap<>();

    paramDeclaration.getParameters().forEach(param -> getAnnotation(param, ValuePart.class)
        .ifPresent(part -> parts.put(param, part.order())));

    return parts;
  }

  private Map<String, String> getContainerParameterNames(List<ParameterDeclaration> allParameters) {
    Map<String, String> parameterNames = new HashMap<>();
    for (ParameterDeclaration parameterDeclaration : allParameters) {
      parameterNames.put(getRealName(parameterDeclaration), parameterDeclaration.getName());
    }
    return parameterNames;
  }
}
