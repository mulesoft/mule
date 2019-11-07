/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Collections.emptyList;
import static org.mule.runtime.app.declaration.internal.utils.Preconditions.checkArgument;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.getSourceElementName;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.resolveConfigName;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.sourceElementNameFromSimpleValue;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.aValueProviderCacheId;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.fromElementWithName;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.connection.ConnectionProviderModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DslElementBasedValueProviderCacheIdGenerator implements ValueProviderCacheIdGenerator<DslElementModel<?>> {

  private ComponentLocator<DslElementModel<?>> locator;

  public DslElementBasedValueProviderCacheIdGenerator(ComponentLocator<DslElementModel<?>> locator) {
    this.locator = locator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(DslElementModel<?> containerComponent, String parameterName) {
    return ifContainsParameter(containerComponent, parameterName).flatMap(
                                                                          parametersMap -> parametersMap.get(parameterName)
                                                                              .getParameterModel().getValueProviderModel()
                                                                              .flatMap(
                                                                                       valueProviderModel -> resolveId(containerComponent,
                                                                                                                       valueProviderModel,
                                                                                                                       parametersMap)));
  }

  private Optional<Map<String, ParameterModelInformation>> ifContainsParameter(DslElementModel<?> containerComponent,
                                                                               String parameterName) {
    return resolveParametersInformation(containerComponent).map(pi -> pi.containsKey(parameterName) ? pi : null);
  }

  private Optional<Map<String, ParameterModelInformation>> resolveParametersInformation(DslElementModel<?> containerComponent) {
    if (containerComponent.getModel() instanceof ParameterizedModel) {
      Map<String, ParameterModelInformation> parametersDslMap = new HashMap<>();
      containerComponent.getContainedElements()
          .stream()
          .filter(containedElement -> containedElement.getModel() instanceof ParameterModel)
          .map(ParameterModelInformation::new)
          .forEach(i -> parametersDslMap.put(i.getParameterModel().getName(), i));
      return of(parametersDslMap);
    }
    return empty();
  }

  private Optional<ValueProviderCacheId> resolveId(DslElementModel<?> containerComponent, ValueProviderModel valueProviderModel,
                                                   Map<String, ParameterModelInformation> parameterModelsInformation) {
    if (containerComponent.getModel() instanceof ComponentModel) {
      return resolveForComponentModel(containerComponent, valueProviderModel, parameterModelsInformation);
    }
    return resolveForGlobalElement(containerComponent, valueProviderModel, parameterModelsInformation);
  }

  private Optional<ValueProviderCacheId> resolveForGlobalElement(DslElementModel<?> containerComponent,
                                                                 ValueProviderModel valueProviderModel,
                                                                 Map<String, ParameterModelInformation> parameterModelsInformation) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    resolveDslTagId(containerComponent).ifPresent(parts::add);

    parts.add(resolveValueProviderId(valueProviderModel));
    parts.addAll(resolveActingParameterIds(valueProviderModel, parameterModelsInformation));

    return of(aValueProviderCacheId(fromElementWithName(getSourceElementName(containerComponent)).containing(parts)));
  }

  private Optional<ValueProviderCacheId> resolveForComponentModel(DslElementModel<?> containerComponent,
                                                                  ValueProviderModel valueProviderModel,
                                                                  Map<String, ParameterModelInformation> parameterModelsInformation) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    resolveDslTagId(containerComponent).ifPresent(parts::add);

    parts.add(resolveValueProviderId(valueProviderModel));
    parts.addAll(resolveActingParameterIds(valueProviderModel, parameterModelsInformation));
    parts.addAll(resolveIdForInjectedElements(containerComponent, valueProviderModel));

    return of(aValueProviderCacheId(fromElementWithName(getSourceElementName(containerComponent)).containing(parts)));

  }

  private List<ValueProviderCacheId> resolveIdForInjectedElements(DslElementModel<?> containerComponent,
                                                                  ValueProviderModel valueProviderModel) {
    if (!valueProviderModel.requiresConfiguration() && !valueProviderModel.requiresConnection()) {
      return emptyList();
    }
    List<ValueProviderCacheId> injectableIds = new LinkedList<>();
    Optional<DslElementModel<?>> configDslElementModel = resolveConfigName(containerComponent)
        .flatMap(config -> locator.get(Location.builder().globalName(config).build()));
    if (configDslElementModel.isPresent() && configDslElementModel.get().getModel() instanceof ConfigurationModel) {
      if (valueProviderModel.requiresConfiguration()) {
        resolveIdForInjectedElement(configDslElementModel.get()).ifPresent(injectableIds::add);
      }
      if (valueProviderModel.requiresConnection()) {
        configDslElementModel.get().getContainedElements().stream()
            .filter(nested -> nested.getModel() instanceof ConnectionProviderModel).forEach(
                                                                                            connectionProvider -> resolveIdForInjectedElement(connectionProvider)
                                                                                                .ifPresent(injectableIds::add));
      }
    }
    return injectableIds;
  }


  private Optional<ValueProviderCacheId> resolveIdForInjectedElement(DslElementModel<?> injectedElement) {
    if (!(injectedElement.getModel() instanceof EnrichableModel)) {
      return empty();
    }
    List<ValueProviderCacheId> parts = new LinkedList<>();
    List<String> parametersRequiredForMetadata =
        ((EnrichableModel) injectedElement.getModel())
            .getModelProperty(RequiredForMetadataModelProperty.class)
            .map(RequiredForMetadataModelProperty::getRequiredParameters)
            .orElse(emptyList());
    resolveParametersInformation(injectedElement)
        .ifPresent(
                   pi -> parametersRequiredForMetadata.forEach(requiredParameter -> {
                     if (pi.containsKey(requiredParameter)) {
                       ParameterModelInformation parameterInfo = pi.get(requiredParameter);
                       resolveParameterId(parameterInfo.getParameterDslElementModel()).ifPresent(parts::add);
                     }
                   }));
    if (parts.isEmpty()) {
      return empty();
    }
    return of(aValueProviderCacheId(fromElementWithName(getSourceElementName(injectedElement)).containing(parts)));
  }

  private ValueProviderCacheId resolveValueProviderId(ValueProviderModel valueProviderModel) {
    return aValueProviderCacheId(fromElementWithName("valueProvider: " + valueProviderModel.getProviderName())
        .withHashValueFrom(valueProviderModel));
  }

  private List<ValueProviderCacheId> resolveActingParameterIds(ValueProviderModel valueProviderModel,
                                                               Map<String, ParameterModelInformation> parameterModelsInformation) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    valueProviderModel.getActingParameters().forEach(
                                                     ap -> {
                                                       if (parameterModelsInformation.containsKey(ap)) {
                                                         resolveParameterId(parameterModelsInformation.get(ap)
                                                             .getParameterDslElementModel()).ifPresent(parts::add);
                                                       }
                                                     });

    return parts;

  }

  private Optional<ValueProviderCacheId> resolveParameterId(DslElementModel<?> parameterModel) {
    return parameterModel.getValue().map(v -> aValueProviderCacheId(
                                                                    fromElementWithName(sourceElementNameFromSimpleValue(parameterModel))
                                                                        .withHashValueFrom(v)));
  }

  private Optional<ValueProviderCacheId> resolveDslTagId(DslElementModel<?> elementModel) {
    return elementModel.getIdentifier()
        .map(id -> aValueProviderCacheId(fromElementWithName(id.toString()).withHashValueFrom(id)));
  }

  private class ParameterModelInformation {

    private ParameterModel parameterModel;
    private DslElementModel<?> parameterDslElementModel;

    private ParameterModelInformation(DslElementModel<?> dslElementModel) {
      checkArgument(dslElementModel.getModel() instanceof ParameterModel, "A ParameterModel is expected");
      this.parameterModel = (ParameterModel) dslElementModel.getModel();
      this.parameterDslElementModel = dslElementModel;
    }

    private ParameterModel getParameterModel() {
      return this.parameterModel;
    }

    private DslElementModel<?> getParameterDslElementModel() {
      return this.parameterDslElementModel;
    }

  }

}
