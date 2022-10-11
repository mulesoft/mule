/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.api;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.metadata.internal.ComponentBasedIdHelper.computeIdFor;
import static org.mule.runtime.metadata.internal.ComponentBasedIdHelper.resolveConfigName;
import static org.mule.runtime.metadata.internal.ComponentBasedIdHelper.sourceElementNameFromSimpleValue;
import static org.mule.runtime.metadata.internal.ComponentBasedIdHelper.sourceElementName;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.aValueProviderCacheId;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.fromElementWithName;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ActingParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.util.cache.CacheIdBuilderAdapter;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.module.extension.internal.value.ValueProviderUtils;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Objects;

/**
 * A {@link ComponentAst} based implementation of a {@link ValueProviderCacheIdGenerator}
 *
 * @since 4.4
 */
public class ComponentAstBasedValueProviderCacheIdGenerator implements ValueProviderCacheIdGenerator<ComponentAst> {

  private final ComponentLocator<ComponentAst> locator;

  private static final String VALUE_PROVIDER = "ValueProvider";

  public ComponentAstBasedValueProviderCacheIdGenerator(ComponentLocator<ComponentAst> locator) {
    this.locator = locator;
  }

  /**
   * {@inheritDoc}
   * <p/>
   * The returned {@link ValueProviderCacheId} will contain all acting parameters required by the
   * {@link org.mule.runtime.extension.api.values.ValueProvider} as parts. In case the {@link ComponentAst} corresponds to a
   * Source or Operation, if the {@link org.mule.runtime.extension.api.values.ValueProvider} requires a connection or a
   * configuration, their id will be added as part. The resolution of a config or connection id as part is different from the one
   * done when their are the one's holding the resolving parameter. In the case they are parts needed by another
   * {@link org.mule.runtime.extension.api.values.ValueProvider}, acting parameters will not exist. Therefore, only parameters
   * required for metadata are used as input to calculate the {@link ValueProviderCacheId}.
   */
  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ComponentAst containerComponent, String parameterName) {
    return ifContainsParameter(containerComponent, parameterName)
        .flatMap(ParameterModel::getValueProviderModel)
        .flatMap(valueProviderModel -> resolveParametersInformation(containerComponent)
            .flatMap(infoMap -> resolveId(containerComponent, valueProviderModel, infoMap)));
  }

  /**
   * {@inheritDoc}
   * <p/>
   * The returned {@link ValueProviderCacheId} will contain all acting parameters required by the
   * {@link org.mule.runtime.extension.api.values.ValueProvider} as parts. In case the {@link ComponentAst} corresponds to a
   * Source or Operation, if the {@link org.mule.runtime.extension.api.values.ValueProvider} requires a connection or a
   * configuration, their id will be added as part. The resolution of a config or connection id as part is different from the one
   * done when their are the one's holding the resolving parameter. In the case they are parts needed by another
   * {@link org.mule.runtime.extension.api.values.ValueProvider}, acting parameters will not exist. Therefore, only parameters
   * required for metadata are used as input to calculate the {@link ValueProviderCacheId}.
   */
  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ComponentAst containerComponent, String parameterName,
                                                               String targetPath) {
    return ifContainsParameter(containerComponent, parameterName)
        .flatMap(pm -> pm.getFieldValueProviderModels()
            .stream()
            .filter(fm -> Objects.equal(fm.getTargetSelector(), targetPath))
            .findAny())
        .flatMap(fieldModel -> resolveParametersInformation(containerComponent)
            .flatMap(infoMap -> resolveId(containerComponent, fieldModel, infoMap)));
  }

  private Optional<ParameterModel> ifContainsParameter(ComponentAst containerComponent, String parameterName) {
    return containerComponent.getModel(ParameterizedModel.class)
        .flatMap(parameterizedModel -> parameterizedModel
            .getAllParameterModels()
            .stream()
            .filter(p -> Objects.equal(parameterName, p.getName()))
            .findAny());
  }

  private Optional<Map<String, ParameterModelInformation>> resolveParametersInformation(ComponentAst containerComponent) {
    return containerComponent.getModel(ParameterizedModel.class)
        .map(parameterizedModel -> containerComponent.getParameters()
            .stream()
            .map(ParameterModelInformation::new)
            .collect(toMap(i -> i.getParameterModel().getName(), identity())));
  }

  private Optional<ValueProviderCacheId> resolveId(ComponentAst containerComponent, ValueProviderModel valueProviderModel,
                                                   Map<String, ParameterModelInformation> parameterModelsInformation) {
    final Optional<ComponentModel> compModel = containerComponent.getModel(ComponentModel.class);

    return compModel
        .map(c -> resolveForComponentModel(containerComponent, valueProviderModel, parameterModelsInformation))
        .orElse(resolveForGlobalElement(containerComponent, valueProviderModel, parameterModelsInformation));
  }

  private Optional<ValueProviderCacheId> resolveForGlobalElement(ComponentAst containerComponent,
                                                                 ValueProviderModel valueProviderModel,
                                                                 Map<String, ParameterModelInformation> parameterModelsInformation) {

    List<ValueProviderCacheId> parts =
        new LinkedList<>(resolveActingParameterIds(containerComponent, valueProviderModel, parameterModelsInformation));
    parts.add(resolveValueProviderId(valueProviderModel));
    parts.add(aValueProviderCacheId(fromElementWithName(VALUE_PROVIDER).withHashValueFrom(VALUE_PROVIDER)));

    String id = sourceElementName(containerComponent);
    return of(aValueProviderCacheId(fromElementWithName(id).withHashValueFrom(resolveDslTagNamespace(containerComponent))
        .containing(parts)));
  }

  private Optional<ValueProviderCacheId> resolveForComponentModel(ComponentAst containerComponent,
                                                                  ValueProviderModel valueProviderModel,
                                                                  Map<String, ParameterModelInformation> parameterModelsInformation) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    parts.addAll(resolveActingParameterIds(containerComponent, valueProviderModel, parameterModelsInformation));
    parts.addAll(resolveIdForInjectedElements(containerComponent, valueProviderModel));
    parts.add(resolveValueProviderId(valueProviderModel));
    parts.add(aValueProviderCacheId(fromElementWithName(VALUE_PROVIDER).withHashValueFrom(VALUE_PROVIDER)));

    String id = containerComponent.getIdentifier().toString();
    return of(aValueProviderCacheId(fromElementWithName(id).withHashValueFrom(resolveDslTagNamespace(containerComponent))
        .containing(parts)));
  }

  private String resolveDslTagNamespace(ComponentAst containerComponent) {
    return containerComponent.getIdentifier().getNamespace().toLowerCase();
  }

  private List<ValueProviderCacheId> resolveIdForInjectedElements(ComponentAst containerComponent,
                                                                  ValueProviderModel valueProviderModel) {
    if (!valueProviderModel.requiresConfiguration() && !valueProviderModel.requiresConnection()) {
      return emptyList();
    }

    return resolveConfigName(containerComponent)
        .flatMap(config -> locator.get(Location.builder().globalName(config).build()))
        .filter(configDslElementModel -> configDslElementModel.getModel(ConfigurationModel.class).isPresent())
        .map(configDslElementModel -> {
          List<ValueProviderCacheId> injectableIds = new LinkedList<>();

          if (valueProviderModel.requiresConfiguration()) {
            resolveIdForInjectedElement(configDslElementModel)
                .ifPresent(id -> injectableIds.add(aValueProviderCacheId(fromElementWithName("config: ").containing(id))));
          }

          if (valueProviderModel.requiresConnection()) {
            configDslElementModel.directChildrenStream()
                .filter(nested -> nested.getModel(ConnectionProviderModel.class).isPresent())
                .forEach(connectionProvider -> resolveIdForInjectedElement(connectionProvider)
                    .ifPresent(id -> injectableIds
                        .add(aValueProviderCacheId(fromElementWithName("connection: ").containing(id)))));
          }

          return injectableIds;
        })
        .orElse(emptyList());
  }


  private Optional<ValueProviderCacheId> resolveIdForInjectedElement(ComponentAst injectedElement) {
    return injectedElement.getModel(EnrichableModel.class)
        .flatMap(enrichableModel -> {
          List<String> parametersRequiredForMetadata =
              enrichableModel
                  .getModelProperty(RequiredForMetadataModelProperty.class)
                  .map(RequiredForMetadataModelProperty::getRequiredParameters)
                  .orElse(emptyList());

          List<ValueProviderCacheId> parts = resolveParametersInformation(injectedElement)
              .map(pi -> parametersRequiredForMetadata
                  .stream()
                  .filter(pi::containsKey)
                  .map(requiredParameter -> resolveParameterId(injectedElement, pi.get(requiredParameter).getParameterAst()))
                  .collect(toList()))
              .orElse(emptyList());

          if (parts.isEmpty()) {
            return empty();
          }

          String sourceElementName = sourceElementNameFromSimpleValue(injectedElement);

          return of(aValueProviderCacheId(fromElementWithName(sourceElementName).withHashValueFrom(sourceElementName)
              .containing(parts)));
        });
  }

  private ValueProviderCacheId resolveValueProviderId(ValueProviderModel valueProviderModel) {
    return aValueProviderCacheId(fromElementWithName("providerId: " + valueProviderModel.getProviderId())
        .withHashValueFrom(valueProviderModel.getProviderId()));
  }

  private List<ValueProviderCacheId> resolveActingParameterIds(ComponentAst containerComponent,
                                                               ValueProviderModel valueProviderModel,
                                                               Map<String, ParameterModelInformation> parameterModelsInformation) {
    return valueProviderModel.getParameters()
        .stream()
        .map(ActingParameterModel::getExtractionExpression)
        .map(ValueProviderUtils::getParameterNameFromExtractionExpression)
        .filter(parameterModelsInformation::containsKey)
        .map(parameterModelsInformation::get)
        .map(ParameterModelInformation::getParameterAst)
        .map(ast -> resolveParameterId(containerComponent, ast))
        .collect(toList());
  }

  private ValueProviderCacheId resolveParameterId(ComponentAst containerComponent,
                                                  ComponentParameterAst componentParameterAst) {
    return computeIdFor(containerComponent, componentParameterAst, ValueProviderCacheIdBuilderAdapter::new);
  }

  private static class ParameterModelInformation {

    private final ComponentParameterAst parameterAst;

    private ParameterModelInformation(ComponentParameterAst p) {
      this.parameterAst = p;
    }

    private ParameterModel getParameterModel() {
      return this.parameterAst.getModel();
    }

    private ComponentParameterAst getParameterAst() {
      return this.parameterAst;
    }

  }

  private static class ValueProviderCacheIdBuilderAdapter implements CacheIdBuilderAdapter<ValueProviderCacheId> {

    private ValueProviderCacheId.ValueProviderCacheIdBuilder builder;

    @Override
    public CacheIdBuilderAdapter<ValueProviderCacheId> withSourceElementName(String name) {
      this.builder = fromElementWithName(name);
      return this;
    }

    @Override
    public CacheIdBuilderAdapter<ValueProviderCacheId> withHashValue(int value) {
      validateInitialized();
      this.builder.withHashValue(value);
      return this;
    }

    @Override
    public CacheIdBuilderAdapter<ValueProviderCacheId> containing(List<ValueProviderCacheId> parts) {
      validateInitialized();
      this.builder.containing(parts);
      return this;
    }

    @Override
    public ValueProviderCacheId build() {
      validateInitialized();
      return aValueProviderCacheId(this.builder);
    }

    private void validateInitialized() {
      if (this.builder == null) {
        throw new RuntimeException("withSourceElementName() never called");
      }
    }

  }


}
