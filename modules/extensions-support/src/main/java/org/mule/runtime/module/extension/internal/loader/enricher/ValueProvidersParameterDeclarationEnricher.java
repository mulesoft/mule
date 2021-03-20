/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getValueProviderId;

import org.mule.runtime.api.meta.model.declaration.fluent.BaseDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConfigurationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ConnectionProviderDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterizedDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.values.ValuePart;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.model.parameter.ImmutableActingParameterModel;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.ParameterGroupDescriptor;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty.ValueProviderFactoryModelPropertyBuilder;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterizableTypeWrapper;
import org.mule.runtime.module.extension.internal.value.EitherOfValue;
import org.mule.runtime.module.extension.internal.value.EitherValueProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@link DeclarationEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for Source, Operation,
 * Configuration and Connection Provider Parameters and ParameterGroups annotated with {@link OfValues} or
 * {@link org.mule.sdk.api.annotation.values.OfValues}. If a parameter or parameter group is annotated, this one will have a
 * related {@link ValueProvider}
 *
 * @since 4.0
 */
public class ValueProvidersParameterDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    Optional<ImplementingTypeModelProperty> implementingType =
        extractImplementingTypeProperty(extensionLoadingContext.getExtensionDeclarer().getDeclaration());

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
  }

  /**
   * This method will look for parameters of the given {@link ParameterizedDeclaration declaration} and, if a parameter or
   * parameter group annotated with {@link OfValues} or {@link org.mule.sdk.api.annotation.values.OfValues} is found, a
   * {@link ValueProviderModel} will be added to this element to communicate that values can be provided.
   * <p>
   * Also the {@link ParameterDeclaration parameters} of the {@link ParameterizedDeclaration declaration} will be enriched.
   *
   * @param containerDeclaration declaration to introspect their parameters
   */
  private void enrichContainerModel(ParameterizedDeclaration<?> containerDeclaration) {
    List<ParameterDeclaration> allParameters = containerDeclaration.getAllParameters();

    Map<String, String> parameterNames = getContainerParameterNames(allParameters);

    Map<ParameterGroupDeclaration, EitherOfValue> dynamicGroupOptions =
        introspectParameterGroups(containerDeclaration.getParameterGroups());

    Map<ParameterDeclaration, EitherOfValue> dynamicOptions = introspectParameters(allParameters);

    dynamicOptions.forEach((paramDeclaration, eitherOfValue) -> enrichParameter(eitherOfValue,
                                                                                paramDeclaration,
                                                                                paramDeclaration::setValueProviderModel,
                                                                                1,
                                                                                parameterNames, paramDeclaration.getName(),
                                                                                allParameters));

    dynamicGroupOptions
        .forEach((paramGroupDeclaration, eitherOfValue) -> getParts(paramGroupDeclaration)
            .forEach((paramDeclaration, order) -> enrichParameter(eitherOfValue, paramDeclaration,
                                                                  paramDeclaration::setValueProviderModel, order, parameterNames,
                                                                  paramGroupDeclaration.getName(), allParameters)));
  }

  /**
   * Enriches a parameter that has an associated {@link ValueProvider}
   *
   * @param eitherOfValue           encapsulation of a {@link ValueProvider} or a {@link org.mule.sdk.api.values.ValueProvider}
   * @param paramDeclaration        {@link ParameterDeclaration} or {@link ParameterGroupDeclaration} paramDeclaration
   * @param containerParameterNames parameters container's names
   */
  private void enrichParameter(EitherOfValue eitherOfValue,
                               BaseDeclaration paramDeclaration,
                               Consumer<ValueProviderModel> valueProviderModelConsumer, Integer partOrder,
                               Map<String, String> containerParameterNames, String name,
                               List<ParameterDeclaration> allParameters) {

    EitherValueProvider eitherValueProvider = new EitherValueProvider(eitherOfValue.value());

    ValueProviderFactoryModelPropertyBuilder propertyBuilder =
        ValueProviderFactoryModelProperty.builder(eitherValueProvider);
    ParameterizableTypeWrapper resolverClassWrapper =
        new ParameterizableTypeWrapper(eitherValueProvider.get(), new DefaultExtensionsTypeLoaderFactory().createTypeLoader());
    List<ExtensionParameter> resolverParameters = resolverClassWrapper.getParametersAnnotatedWith(Parameter.class);

    resolverParameters.forEach(param -> propertyBuilder
        .withInjectableParameter(param.getName(), param.getType().asMetadataType(), param.isRequired()));

    Reference<Boolean> requiresConfiguration = new Reference<>(false);
    Reference<Boolean> requiresConnection = new Reference<>(false);

    enrichWithConnection(propertyBuilder, resolverClassWrapper)
        .ifPresent(field -> requiresConnection.set(true));
    enrichWithConfiguration(propertyBuilder, resolverClassWrapper)
        .ifPresent(field -> requiresConfiguration.set(true));

    paramDeclaration.addModelProperty(propertyBuilder.build());

    valueProviderModelConsumer
        .accept(new ValueProviderModel(getActingParametersModel(resolverParameters, containerParameterNames, allParameters),
                                       requiresConfiguration.get(), requiresConnection.get(), eitherOfValue.open(), partOrder,
                                       name, getValueProviderId(eitherValueProvider)));
  }

  /**
   * Introspects the given {@link ParameterizableTypeWrapper parameterizableComponent} looking if this ones uses a
   * {@link Connection}, if this is true this method will indicate to the {@link ValueProviderFactoryModelPropertyBuilder} that
   * the correspondent {@link ValueProvider} will require a connection.
   *
   * @param modelPropertyBuilder     Options Resolver Model Property Builder
   * @param parameterizableComponent component to introspect
   */
  private Optional<Field> enrichWithConnection(ValueProviderFactoryModelPropertyBuilder modelPropertyBuilder,
                                               ParameterizableTypeWrapper parameterizableComponent) {
    List<FieldElement> connectionFields = parameterizableComponent.getAnnotatedFields(Connection.class);
    if (!connectionFields.isEmpty()) {
      Field field = connectionFields.get(0).getField().get();
      modelPropertyBuilder.withConnection(field);
      return of(field);
    }
    return empty();
  }

  /**
   * Introspects the given {@link ParameterizableTypeWrapper parameterizableComponent} looking if this ones uses a {@link Config},
   * if this is true this method will indicate to the {@link ValueProviderFactoryModelPropertyBuilder} that the correspondent
   * {@link ValueProvider} will require a config.
   *
   * @param modelPropertyBuilder     Options Resolver Model Property Builder
   * @param parameterizableComponent component to introspect
   */
  private Optional<Field> enrichWithConfiguration(ValueProviderFactoryModelPropertyBuilder modelPropertyBuilder,
                                                  ParameterizableTypeWrapper parameterizableComponent) {
    List<FieldElement> configFields = parameterizableComponent.getAnnotatedFields(Config.class);
    if (!configFields.isEmpty()) {
      Field field = configFields.get(0).getField().get();
      modelPropertyBuilder.withConfig(field);
      return of(field);
    }
    return empty();
  }

  /**
   * Given a list of {@link ParameterDeclaration}, introspect it and finds all the considered parameters with an associated
   * {@link ValueProvider} or {@link org.mule.sdk.api.values.ValueProvider}
   *
   * @param parameters parameters to introspect
   * @return a Map containing all the {@link ParameterDeclaration} with their correspondent {@link ValueProvider}
   */
  private Map<ParameterDeclaration, EitherOfValue> introspectParameters(List<ParameterDeclaration> parameters) {

    Map<ParameterDeclaration, EitherOfValue> optionResolverEnabledParameters = new HashMap<>();

    parameters.forEach(param -> getOfValueAnnotation(param)
        .ifPresent(optionAnnotation -> optionResolverEnabledParameters.put(param, new EitherOfValue(optionAnnotation))));

    return optionResolverEnabledParameters;
  }

  /**
   * Given a list of {@link ParameterGroupDeclaration}, introspect it and finds all the considered parameters with an associated
   * {@link ValueProvider} or {@link org.mule.sdk.api.values.ValueProvider}
   *
   * @param parameterGroups parameter groups to introspect
   * @return a Map containing all the {@link ParameterGroupDeclaration} with their correspondent {@link ValueProvider}
   */
  private Map<ParameterGroupDeclaration, EitherOfValue> introspectParameterGroups(List<ParameterGroupDeclaration> parameterGroups) {
    Map<ParameterGroupDeclaration, EitherOfValue> optionResolverEnabledParameters = new HashMap<>();

    parameterGroups
        .forEach(paramGroup -> paramGroup.getModelProperty(ParameterGroupModelProperty.class)
            .ifPresent(modelProperty -> {

              AnnotatedElement container = modelProperty.getDescriptor().getContainer();
              if (container != null) {
                getOfValueAnnotation(paramGroup, container)
                    .ifPresent(v -> optionResolverEnabledParameters.put(paramGroup, new EitherOfValue(v)));
              }
            }));

    return optionResolverEnabledParameters;
  }

  private <T extends Annotation> Optional<T> getAnnotation(ParameterDeclaration param, Class<T> annotationClass) {
    return getAnnotatedElement(param)
        .map(annotatedElement -> ofNullable(annotatedElement.getAnnotation(annotationClass)))
        .orElse(empty());
  }

  private List<ActingParameterModel> getActingParametersModel(List<ExtensionParameter> parameterDeclarations,
                                                              Map<String, String> parameterNames,
                                                              List<ParameterDeclaration> allParameters) {
    Map<String, Boolean> paramsInfo = parameterDeclarations.stream()
        .collect(toMap(param -> parameterNames.getOrDefault(param.getName(), param.getName()), ExtensionParameter::isRequired));
    return allParameters.stream()
        .filter(param -> paramsInfo.containsKey(param.getName()))
        .map(param -> new ImmutableActingParameterModel(param.getName(), paramsInfo.get(param.getName())))
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
      parameterNames.put(getImplementingName(parameterDeclaration), parameterDeclaration.getName());
    }
    return parameterNames;
  }

  private Optional<?> getOfValueAnnotation(ParameterDeclaration parameterDeclaration) {
    Optional<OfValues> legacyAnnotation = getAnnotation(parameterDeclaration, OfValues.class);
    Optional<org.mule.sdk.api.annotation.values.OfValues> sdkAnnotation =
        getAnnotation(parameterDeclaration, org.mule.sdk.api.annotation.values.OfValues.class);

    if (legacyAnnotation.isPresent() && sdkAnnotation.isPresent()) {
      throw new IllegalModelDefinitionException(format("Parameter %s cannot be annotated with both %s and %s",
                                                       parameterDeclaration.getName(),
                                                       OfValues.class.getName(),
                                                       org.mule.sdk.api.annotation.values.OfValues.class.getName()));
    } else if (legacyAnnotation.isPresent()) {
      return legacyAnnotation;
    } else {
      return sdkAnnotation;
    }
  }

  private Optional<?> getOfValueAnnotation(ParameterGroupDeclaration parameterGroupDeclaration,
                                           AnnotatedElement annotatedElement) {
    OfValues legacyAnnotation = annotatedElement.getAnnotation(OfValues.class);
    org.mule.sdk.api.annotation.values.OfValues sdkAnnotation =
        annotatedElement.getAnnotation(org.mule.sdk.api.annotation.values.OfValues.class);

    if (legacyAnnotation != null && sdkAnnotation != null) {
      throw new IllegalModelDefinitionException(format("Parameter group %s cannot be annotated with both %s and %s",
                                                       parameterGroupDeclaration.getName(),
                                                       OfValues.class.getName(),
                                                       org.mule.sdk.api.annotation.values.OfValues.class.getName()));
    } else if (legacyAnnotation != null) {
      return of(legacyAnnotation);
    } else {
      return ofNullable(sdkAnnotation);
    }
  }

}
