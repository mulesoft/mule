/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.enricher;

import static org.mule.runtime.module.extension.internal.loader.parser.java.MuleExtensionAnnotationParser.mapReduceAnnotation;
import static org.mule.runtime.module.extension.internal.loader.utils.FieldValueProviderNameUtils.getFieldValueProviderName;
import static org.mule.runtime.module.extension.internal.loader.utils.ParameterUtils.getConfigFields;
import static org.mule.runtime.module.extension.internal.loader.utils.ParameterUtils.getConnectionFields;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.extractImplementingTypeProperty;
import static org.mule.runtime.module.extension.internal.value.ValueProviderUtils.getValueProviderId;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

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
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.FieldValueProviderModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.values.ValuePart;
import org.mule.runtime.extension.api.exception.IllegalModelDefinitionException;
import org.mule.runtime.extension.api.exception.IllegalParameterModelDefinitionException;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.loader.IdempotentDeclarationEnricherWalkDelegate;
import org.mule.runtime.extension.api.loader.WalkingDeclarationEnricher;
import org.mule.runtime.extension.api.model.parameter.ImmutableActingParameterModel;
import org.mule.runtime.extension.api.property.SinceMuleVersionModelProperty;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.property.FieldsValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ValueProviderFactoryModelProperty.ValueProviderFactoryModelPropertyBuilder;
import org.mule.runtime.module.extension.internal.loader.java.type.property.ExtensionParameterDescriptorModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterizableTypeWrapper;
import org.mule.runtime.module.extension.internal.value.OfValueInformation;
import org.mule.sdk.api.annotation.binding.Binding;
import org.mule.sdk.api.annotation.values.FieldValues;
import org.mule.sdk.api.annotation.values.FieldsValues;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * {@link DeclarationEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for Source, Operation,
 * Configuration and Connection Provider Parameters and ParameterGroups annotated with {@link OfValues} or
 * {@link org.mule.sdk.api.annotation.values.OfValues}. If a parameter or parameter group is annotated, this one will have a
 * related {@link org.mule.sdk.api.values.Value}
 *
 * @since 4.0
 */
public class ValueProvidersParameterDeclarationEnricher implements WalkingDeclarationEnricher {

  private static final SinceMuleVersionModelProperty SINCE_MULE_VERSION_MODEL_PROPERTY_SDK_API_VP =
      new SinceMuleVersionModelProperty("4.4.0");

  @Override
  public Optional<DeclarationEnricherWalkDelegate> getWalkDelegate(ExtensionLoadingContext extensionLoadingContext) {
    ClassTypeLoader classTypeLoader = extensionLoadingContext.getTypeLoader();
    return extractImplementingTypeProperty(extensionLoadingContext.getExtensionDeclarer().getDeclaration())
        .map(p -> new IdempotentDeclarationEnricherWalkDelegate() {

          @Override
          public void onSource(SourceDeclaration declaration) {
            enrichContainerModel(declaration, declaration.getName(), "source", classTypeLoader);
          }

          @Override
          public void onOperation(OperationDeclaration declaration) {
            enrichContainerModel(declaration, declaration.getName(), "operation", classTypeLoader);
          }

          @Override
          public void onConfiguration(ConfigurationDeclaration declaration) {
            enrichContainerModel(declaration, declaration.getName(), "configuration", classTypeLoader);
          }

          @Override
          protected void onConnectionProvider(ConnectionProviderDeclaration declaration) {
            enrichContainerModel(declaration, declaration.getName(), "connection provider", classTypeLoader);
          }
        });
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
  private void enrichContainerModel(ParameterizedDeclaration<?> containerDeclaration, String componentName,
                                    String componentType,
                                    ClassTypeLoader classTypeLoader) {
    List<ParameterDeclaration> allParameters = containerDeclaration.getAllParameters();

    Map<String, String> parameterNames = getContainerParameterNames(allParameters);

    Map<ParameterGroupDeclaration, OfValueInformation> dynamicGroupOptions =
        introspectParameterGroups(containerDeclaration.getParameterGroups(), componentName, componentType);

    Map<ParameterDeclaration, OfValueInformation> dynamicOptions =
        introspectParameters(allParameters, componentName, componentType);

    Map<ParameterDeclaration, List<FieldValues>> dynamicFieldOptions = introspectParameterFields(allParameters);

    dynamicOptions.forEach((paramDeclaration, ofValueInformation) -> enrichParameter(ofValueInformation,
                                                                                     paramDeclaration,
                                                                                     paramDeclaration::setValueProviderModel,
                                                                                     1,
                                                                                     parameterNames, paramDeclaration.getName(),
                                                                                     allParameters,
                                                                                     classTypeLoader));

    dynamicFieldOptions.forEach((paramDeclaration, fieldsValues) -> enrichParameterFields(fieldsValues,
                                                                                          paramDeclaration,
                                                                                          parameterNames,
                                                                                          paramDeclaration.getName(),
                                                                                          allParameters,
                                                                                          classTypeLoader));

    dynamicGroupOptions
        .forEach((paramGroupDeclaration, ofValueInformation) -> getParts(paramGroupDeclaration)
            .forEach((paramDeclaration, order) -> enrichParameter(ofValueInformation, paramDeclaration,
                                                                  paramDeclaration::setValueProviderModel, order, parameterNames,
                                                                  paramGroupDeclaration.getName(), allParameters,
                                                                  classTypeLoader)));
  }

  /**
   * Enriches a parameter that has an associated {@link ValueProvider}
   *
   * @param ofValueInformation      encapsulation of a {@link ValueProvider} or a {@link org.mule.sdk.api.values.ValueProvider}
   * @param paramDeclaration        {@link ParameterDeclaration} or {@link ParameterGroupDeclaration} paramDeclaration
   * @param containerParameterNames parameters container's names
   */
  private void enrichParameter(OfValueInformation ofValueInformation,
                               BaseDeclaration paramDeclaration,
                               Consumer<ValueProviderModel> valueProviderModelConsumer, Integer partOrder,
                               Map<String, String> containerParameterNames, String name,
                               List<ParameterDeclaration> allParameters,
                               ClassTypeLoader classTypeLoader) {
    Map<String, String> bindingMap = getBindingsMap(ofValueInformation.getBindings());

    ValueProviderFactoryModelPropertyBuilder propertyBuilder =
        ValueProviderFactoryModelProperty.builder(ofValueInformation.getValue());
    ParameterizableTypeWrapper resolverClassWrapper =
        new ParameterizableTypeWrapper(ofValueInformation.getValue(), classTypeLoader);
    List<ExtensionParameter> resolverParameters = resolverClassWrapper.getParametersAnnotatedWith(Parameter.class);

    resolverParameters.forEach(param -> propertyBuilder
        .withInjectableParameter(param.getName(), param.getType().asMetadataType(), param.isRequired(),
                                 bindingMap
                                     .getOrDefault(param.getName(),
                                                   containerParameterNames.getOrDefault(param.getName(), param.getName()))));

    Reference<Boolean> requiresConfiguration = new Reference<>(false);
    Reference<Boolean> requiresConnection = new Reference<>(false);

    enrichWithConnection(propertyBuilder, resolverClassWrapper)
        .ifPresent(field -> requiresConnection.set(true));
    enrichWithConfiguration(propertyBuilder, resolverClassWrapper)
        .ifPresent(field -> requiresConfiguration.set(true));

    paramDeclaration.addModelProperty(propertyBuilder.build());

    if (ofValueInformation.isFromLegacyAnnotation()) {
      valueProviderModelConsumer
          .accept(new ValueProviderModel(getActingParametersModel(resolverParameters, containerParameterNames, allParameters,
                                                                  bindingMap),
                                         requiresConfiguration.get(), requiresConnection.get(), ofValueInformation.isOpen(),
                                         partOrder,
                                         name, getValueProviderId(ofValueInformation.getValue())));
    } else {
      valueProviderModelConsumer
          .accept(new ValueProviderModel(getActingParametersModel(resolverParameters, containerParameterNames, allParameters,
                                                                  bindingMap),
                                         requiresConfiguration.get(), requiresConnection.get(), ofValueInformation.isOpen(),
                                         partOrder,
                                         name, getValueProviderId(ofValueInformation.getValue()),
                                         SINCE_MULE_VERSION_MODEL_PROPERTY_SDK_API_VP));
    }

  }

  private Map<String, String> getBindingsMap(Binding[] bindings) {
    Map<String, String> bindingsMap = new HashMap<>();
    for (Binding binding : bindings) {
      bindingsMap.put(binding.actingParameter(), binding.extractionExpression());
    }
    return bindingsMap;
  }

  private void enrichParameterFields(List<FieldValues> fieldsValues, ParameterDeclaration paramDeclaration,
                                     Map<String, String> parameterNames, String name, List<ParameterDeclaration> allParameters,
                                     ClassTypeLoader classTypeLoader) {

    List<FieldValueProviderModel> fieldValueProviderModels = new LinkedList<>();
    Map<String, ValueProviderFactoryModelProperty> valueProviderFactoryModelProperties = new HashMap<>();

    for (FieldValues fieldValues : fieldsValues) {
      Map<String, String> bindingsMap = getBindingsMap(fieldValues.bindings());
      ValueProviderFactoryModelPropertyBuilder propertyBuilder =
          ValueProviderFactoryModelProperty.builder(fieldValues.value());

      ParameterizableTypeWrapper resolverClassWrapper = new ParameterizableTypeWrapper(fieldValues.value(), classTypeLoader);
      List<ExtensionParameter> resolverParameters = resolverClassWrapper.getParametersAnnotatedWith(Parameter.class);

      resolverParameters.forEach(param -> propertyBuilder
          .withInjectableParameter(param.getName(), param.getType().asMetadataType(), param.isRequired(), bindingsMap
              .getOrDefault(param.getName(),
                            parameterNames.getOrDefault(param.getName(), param.getName()))));

      Reference<Boolean> requiresConfiguration = new Reference<>(false);
      Reference<Boolean> requiresConnection = new Reference<>(false);

      enrichWithConnection(propertyBuilder, resolverClassWrapper)
          .ifPresent(field -> requiresConnection.set(true));
      enrichWithConfiguration(propertyBuilder, resolverClassWrapper)
          .ifPresent(field -> requiresConfiguration.set(true));

      int partOrder = 1;
      String providerName = getFieldValueProviderName(name, fieldValues.targetSelectors());
      for (String targetSelector : fieldValues.targetSelectors()) {
        ValueProviderFactoryModelProperty valueProviderFactoryModelProperty = propertyBuilder.build();
        valueProviderFactoryModelProperties.put(targetSelector, valueProviderFactoryModelProperty);
        fieldValueProviderModels
            .add(new FieldValueProviderModel(getActingParametersModel(resolverParameters, parameterNames, allParameters,
                                                                      bindingsMap),
                                             requiresConfiguration.get(), requiresConnection.get(), fieldValues.open(),
                                             partOrder,
                                             providerName, getValueProviderId(fieldValues.value()), targetSelector));
        partOrder++;
      }
      paramDeclaration.setFieldValueProviderModels(fieldValueProviderModels);
      paramDeclaration.addModelProperty(new FieldsValueProviderFactoryModelProperty(valueProviderFactoryModelProperties));
    }
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
    List<FieldElement> connectionFields = getConnectionFields(parameterizableComponent);
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
    List<FieldElement> configFields = getConfigFields(parameterizableComponent);
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
  private Map<ParameterDeclaration, OfValueInformation> introspectParameters(List<ParameterDeclaration> parameters,
                                                                             String componentName, String componentType) {

    Map<ParameterDeclaration, OfValueInformation> optionResolverEnabledParameters = new HashMap<>();

    parameters.forEach(param -> getOfValueInformation(param, componentName, componentType)
        .ifPresent(optionAnnotation -> optionResolverEnabledParameters.put(param, optionAnnotation)));

    return optionResolverEnabledParameters;
  }

  /**
   * Given a list of {@link ParameterGroupDeclaration}, introspect it and finds all the considered parameters with an associated
   * {@link ValueProvider} or {@link org.mule.sdk.api.values.ValueProvider}
   *
   * @param parameterGroups parameter groups to introspect
   * @return a Map containing all the {@link ParameterGroupDeclaration} with their correspondent {@link ValueProvider}
   */
  private Map<ParameterGroupDeclaration, OfValueInformation> introspectParameterGroups(List<ParameterGroupDeclaration> parameterGroups,
                                                                                       String componentName,
                                                                                       String componentType) {
    Map<ParameterGroupDeclaration, OfValueInformation> optionResolverEnabledParameters = new HashMap<>();

    parameterGroups
        .forEach(paramGroup -> paramGroup.getModelProperty(ParameterGroupModelProperty.class)
            .ifPresent(modelProperty -> {
              AnnotatedElement container = modelProperty.getDescriptor().getContainer();
              if (container != null) {
                getOfValueInformation(paramGroup, container, componentName, componentType)
                    .ifPresent(v -> optionResolverEnabledParameters.put(paramGroup, v));
              }
            }));

    return optionResolverEnabledParameters;
  }

  private Map<ParameterDeclaration, List<FieldValues>> introspectParameterFields(List<ParameterDeclaration> parameters) {
    Map<ParameterDeclaration, List<FieldValues>> optionResolverEnabledParameters = new HashMap<>();

    parameters.forEach(param -> {
      List<FieldValues> fieldValuesList = new ArrayList<>();
      getAnnotation(param, FieldsValues.class)
          .ifPresent(optionAnnotation -> fieldValuesList.addAll(asList(optionAnnotation.value())));
      getAnnotation(param, FieldValues.class).ifPresent(fieldValuesList::add);

      optionResolverEnabledParameters.put(param, fieldValuesList);
    });

    return optionResolverEnabledParameters;
  }

  private <T extends Annotation> Optional<T> getAnnotation(ParameterDeclaration param, Class<T> annotationClass) {
    return getAnnotatedElement(param)
        .map(annotatedElement -> ofNullable(annotatedElement.getAnnotation(annotationClass)))
        .orElse(empty());
  }

  private List<ActingParameterModel> getActingParametersModel(List<ExtensionParameter> parameterDeclarations,
                                                              Map<String, String> parameterNames,
                                                              List<ParameterDeclaration> allParameters,
                                                              Map<String, String> bindings) {
    return parameterDeclarations.stream()
        .map(extensionParameter -> {
          if (bindings.containsKey(extensionParameter.getName())) {
            return new ImmutableActingParameterModel(extensionParameter.getName(),
                                                     extensionParameter.isRequired(),
                                                     bindings.get(extensionParameter.getName()));
          } else {
            return new ImmutableActingParameterModel(parameterNames
                .getOrDefault(extensionParameter.getName(), extensionParameter.getName()), extensionParameter.isRequired(),
                                                     parameterNames.getOrDefault(extensionParameter.getName(),
                                                                                 extensionParameter.getName()));
          }
        })
        .collect(toList());
  }

  private Map<ParameterDeclaration, Integer> getParts(ParameterGroupDeclaration paramDeclaration) {
    Map<ParameterDeclaration, Integer> parts = new HashMap<>();

    paramDeclaration.getParameters()
        .forEach(param -> param.getModelProperty(ExtensionParameterDescriptorModelProperty.class)
            .ifPresent(extensionParameterDescriptorModelProperty -> mapReduceAnnotation(extensionParameterDescriptorModelProperty
                .getExtensionParameter(),
                                                                                        ValuePart.class,
                                                                                        org.mule.sdk.api.annotation.values.ValuePart.class,
                                                                                        value -> value
                                                                                            .getNumberValue(ValuePart::order),
                                                                                        value -> value
                                                                                            .getNumberValue(org.mule.sdk.api.annotation.values.ValuePart::order),
                                                                                        () -> new IllegalParameterModelDefinitionException(format("Parameter '%s' is annotated with '@%s' and '@%s' at the same time",
                                                                                                                                                  param
                                                                                                                                                      .getName(),
                                                                                                                                                  ValuePart.class
                                                                                                                                                      .getName(),
                                                                                                                                                  org.mule.sdk.api.annotation.values.ValuePart.class
                                                                                                                                                      .getName())))
                .ifPresent(order -> parts
                    .put(param,
                         order))));

    return parts;
  }

  private Map<String, String> getContainerParameterNames(List<ParameterDeclaration> allParameters) {
    Map<String, String> parameterNames = new HashMap<>();
    for (ParameterDeclaration parameterDeclaration : allParameters) {
      parameterNames.put(getImplementingName(parameterDeclaration), parameterDeclaration.getName());
    }
    return parameterNames;
  }

  private Optional<OfValueInformation> getOfValueInformation(ParameterDeclaration parameterDeclaration, String componentName,
                                                             String componentType) {
    Optional<OfValues> legacyAnnotation = getAnnotation(parameterDeclaration, OfValues.class);
    Optional<org.mule.sdk.api.annotation.values.OfValues> sdkAnnotation =
        getAnnotation(parameterDeclaration, org.mule.sdk.api.annotation.values.OfValues.class);

    return getOfValueInformation(legacyAnnotation.orElse(null), sdkAnnotation.orElse(null), parameterDeclaration.getName(),
                                 componentName, componentType, "parameter");
  }

  private Optional<OfValueInformation> getOfValueInformation(ParameterGroupDeclaration parameterGroupDeclaration,
                                                             AnnotatedElement annotatedElement,
                                                             String componentName,
                                                             String componentType) {
    OfValues legacyAnnotation = annotatedElement.getAnnotation(OfValues.class);
    org.mule.sdk.api.annotation.values.OfValues sdkAnnotation =
        annotatedElement.getAnnotation(org.mule.sdk.api.annotation.values.OfValues.class);

    return getOfValueInformation(legacyAnnotation, sdkAnnotation, parameterGroupDeclaration.getName(), componentName,
                                 componentType, "parameter group");
  }

  private Optional<OfValueInformation> getOfValueInformation(OfValues legacyOfValues,
                                                             org.mule.sdk.api.annotation.values.OfValues ofValues,
                                                             String elementName,
                                                             String componentName,
                                                             String componentType,
                                                             String elementType) {
    if (legacyOfValues != null && ofValues != null) {
      throw new IllegalModelDefinitionException(format("Annotations %s and %s are both present at the same time on %s %s of %s %s",
                                                       OfValues.class.getName(),
                                                       org.mule.sdk.api.annotation.values.OfValues.class.getName(),
                                                       elementType,
                                                       elementName,
                                                       componentType,
                                                       componentName));
    } else if (legacyOfValues != null) {
      return of(new OfValueInformation(legacyOfValues.value(), legacyOfValues.open(), new Binding[0], true));
    } else if (ofValues != null) {
      return of(new OfValueInformation(ofValues.value(), ofValues.open(), ofValues.bindings(), false));
    } else {
      return empty();
    }
  }

}
