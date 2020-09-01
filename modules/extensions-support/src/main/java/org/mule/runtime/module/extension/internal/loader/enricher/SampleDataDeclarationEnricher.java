/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.enricher;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.module.extension.internal.data.sample.SampleDataUtils.getSampleDataProviderId;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getAnnotatedElement;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getImplementingName;

import org.mule.runtime.api.meta.model.data.sample.SampleDataProviderModel;
import org.mule.runtime.api.meta.model.declaration.fluent.ExecutableComponentDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ExtensionDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.OperationDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.ParameterGroupDeclaration;
import org.mule.runtime.api.meta.model.declaration.fluent.SourceDeclaration;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.Connection;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.values.OfValues;
import org.mule.runtime.extension.api.annotation.values.ValuePart;
import org.mule.runtime.extension.api.declaration.fluent.util.IdempotentDeclarationWalker;
import org.mule.runtime.extension.api.declaration.type.DefaultExtensionsTypeLoaderFactory;
import org.mule.runtime.extension.api.loader.DeclarationEnricher;
import org.mule.runtime.extension.api.loader.ExtensionLoadingContext;
import org.mule.runtime.extension.api.values.ValueProvider;
import org.mule.runtime.module.extension.api.loader.java.type.ExtensionParameter;
import org.mule.runtime.module.extension.api.loader.java.type.FieldElement;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingMethodModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ImplementingTypeModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.ParameterGroupModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.SampleDataProviderFactoryModelProperty.SampleDataProviderFactoryModelPropertyBuilder;
import org.mule.runtime.module.extension.internal.loader.java.type.runtime.ParameterizableTypeWrapper;
import org.mule.sdk.api.annotation.data.sample.SampleData;
import org.mule.sdk.api.data.sample.SampleDataProvider;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * {@link DeclarationEnricher} implementation that walks through a {@link ExtensionDeclaration} and looks for Source,
 * Operation, Configuration and Connection Provider Parameters and ParameterGroups annotated with {@link OfValues}.
 * If a parameter or parameter group is annotated, this one will have a related {@link ValueProvider}
 *
 * @since 4.4.0
 */
public class SampleDataDeclarationEnricher extends AbstractAnnotatedDeclarationEnricher {

  @Override
  public void enrich(ExtensionLoadingContext extensionLoadingContext) {
    new IdempotentDeclarationWalker() {

      @Override
      public void onSource(SourceDeclaration declaration) {
        declaration.getModelProperty(ImplementingTypeModelProperty.class).ifPresent(property -> {
          SampleData annotation = property.getType().getAnnotation(SampleData.class);
          if (annotation != null) {
            declaration.setSampleDataProviderModel(createSampleDataModel(annotation, declaration));
          }
        });
      }

      @Override
      public void onOperation(OperationDeclaration declaration) {
        declaration.getModelProperty(ImplementingMethodModelProperty.class).ifPresent(property -> {
          Method method = property.getMethod();
          SampleData annotation = method.getAnnotation(SampleData.class);
          if (annotation != null) {
            declaration.setSampleDataProviderModel(createSampleDataModel(annotation, declaration));
          }
        });
      }
    }.walk(extensionLoadingContext.getExtensionDeclarer().getDeclaration());
  }

  private SampleDataProviderModel createSampleDataModel(SampleData annotation, ExecutableComponentDeclaration declaration) {

    List<ParameterDeclaration> allParameters = declaration.getAllParameters();
    Map<String, String> parameterNames = getContainerParameterNames(allParameters);

    Class<? extends SampleDataProvider> resolverClass = annotation.value();
    SampleDataProviderFactoryModelPropertyBuilder propertyBuilder = new SampleDataProviderFactoryModelPropertyBuilder(resolverClass);

    ParameterizableTypeWrapper resolverClassWrapper =
            new ParameterizableTypeWrapper(resolverClass, new DefaultExtensionsTypeLoaderFactory().createTypeLoader());

    List<ExtensionParameter> resolverParameters = resolverClassWrapper.getParametersAnnotatedWith(Parameter.class);
    resolverParameters.addAll(resolverClassWrapper.getParametersAnnotatedWith(org.mule.sdk.api.annotation.param.Parameter.class));

    resolverParameters.forEach(param -> propertyBuilder
            .withInjectableParameter(param.getName(), param.getType().asMetadataType(), param.isRequired()));

    Reference<Boolean> requiresConfiguration = new Reference<>(false);
    Reference<Boolean> requiresConnection = new Reference<>(false);

    enrichWithConnection(propertyBuilder, resolverClassWrapper).ifPresent(field -> requiresConnection.set(true));
    enrichWithConfiguration(propertyBuilder, resolverClassWrapper).ifPresent(field -> requiresConfiguration.set(true));

    declaration.addModelProperty(propertyBuilder.build());

    return new SampleDataProviderModel(
            getRequiredParametersAliases(resolverParameters, parameterNames),
            getSampleDataProviderId(resolverClass),
            requiresConfiguration.get(),
            requiresConnection.get());
  }

  /**
   * Introspects the given {@link ParameterizableTypeWrapper parameterizableComponent} looking if this ones uses either a
   * {@link Connection} or {@link org.mule.sdk.api.annotation.param.Connection}. If any is true this method will indicate
   * to the {@link SampleDataProviderFactoryModelPropertyBuilder}
   * that the correspondent {@link SampleDataProvider} will require a connection.
   *
   * @param modelPropertyBuilder     the model property builder
   * @param parameterizableComponent component to introspect
   */
  private Optional<Field> enrichWithConnection(SampleDataProviderFactoryModelPropertyBuilder modelPropertyBuilder,
                                               ParameterizableTypeWrapper parameterizableComponent) {
    List<FieldElement> connectionFields = parameterizableComponent.getAnnotatedFields(Connection.class,
            org.mule.sdk.api.annotation.param.Connection.class);
    if (!connectionFields.isEmpty()) {
      Field field = connectionFields.get(0).getField().get();
      modelPropertyBuilder.withConnection(field);
      return of(field);
    }
    return empty();
  }

  /**
   * Introspects the given {@link ParameterizableTypeWrapper parameterizableComponent} looking if this ones uses either a
   * {@link Config} or {@link org.mule.sdk.api.annotation.param.Config}, if any is true this method will indicate to the
   * {@link SampleDataProviderFactoryModelPropertyBuilder} that the correspondent {@link SampleDataProvider} will require
   * a config.
   *
   * @param modelPropertyBuilder     the property builder
   * @param parameterizableComponent component to introspect
   */
  private Optional<Field> enrichWithConfiguration(SampleDataProviderFactoryModelPropertyBuilder modelPropertyBuilder,
                                                  ParameterizableTypeWrapper parameterizableComponent) {
    List<FieldElement> configFields = parameterizableComponent.getAnnotatedFields(Config.class,
            org.mule.sdk.api.annotation.param.Config.class);
    if (!configFields.isEmpty()) {
      Field field = configFields.get(0).getField().get();
      modelPropertyBuilder.withConfig(field);
      return of(field);
    }
    return empty();
  }

  /**
   * Given a list of {@link ParameterGroupDeclaration}, introspect it and finds all the considered parameters with an associated
   * {@link ValueProvider}
   *
   * @param parameterGroups parameter groups to introspect
   * @return a Map containing all the {@link ParameterGroupDeclaration} with their correspondent {@link ValueProvider}
   */
  private Map<ParameterGroupDeclaration, OfValues> introspectParameterGroups(List<ParameterGroupDeclaration> parameterGroups) {
    Map<ParameterGroupDeclaration, OfValues> optionResolverEnabledParameters = new HashMap<>();

    parameterGroups
            .forEach(paramGroup -> paramGroup.getModelProperty(ParameterGroupModelProperty.class)
                    .ifPresent(modelProperty -> {

                      AnnotatedElement container = modelProperty.getDescriptor().getContainer();
                      if (container != null) {
                        OfValues annotation = container.getAnnotation(OfValues.class);
                        if (annotation != null) {
                          optionResolverEnabledParameters.put(paramGroup, annotation);
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
      parameterNames.put(getImplementingName(parameterDeclaration), parameterDeclaration.getName());
    }
    return parameterNames;
  }
}
