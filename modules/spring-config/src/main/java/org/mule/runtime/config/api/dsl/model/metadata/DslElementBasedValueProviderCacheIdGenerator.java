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
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.resolveSimpleValue;
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

import com.google.common.base.Objects;

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
   * <p/>
   * The returned {@link ValueProviderCacheId} will contain all acting parameters required by the {@link org.mule.runtime.extension.api.values.ValueProvider} as parts.
   * In case the {@link DslElementModel} corresponds to a Source or Operation, if the {@link org.mule.runtime.extension.api.values.ValueProvider} requires a connection or
   * a configuration, their id will be added as part.
   * The resolution of a config or connection id as part is different from the one done when their are the one's holding the resolving parameter.
   * In the case they are parts needed by another {@link org.mule.runtime.extension.api.values.ValueProvider}, acting parameters will not exist. Therefore, only
   * parameters required for metadata are used as input to calculate the {@link ValueProviderCacheId}.
   */
  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(DslElementModel<?> containerComponent, String parameterName) {
    return ifContainsParameter(containerComponent, parameterName)
        .flatMap(ParameterModel::getValueProviderModel)
        .flatMap(valueProviderModel -> resolveParametersInformation(containerComponent)
            .flatMap(infoMap -> resolveId(containerComponent, valueProviderModel, infoMap)));
  }

  private Optional<ParameterModel> ifContainsParameter(DslElementModel<?> containerComponent, String parameterName) {
    if (containerComponent.getModel() instanceof ParameterizedModel) {
      return ((ParameterizedModel) containerComponent.getModel())
          .getAllParameterModels()
          .stream()
          .filter(p -> Objects.equal(parameterName, p.getName()))
          .findAny();
    }
    return empty();
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

    parts.add(resolveValueProviderId(valueProviderModel));
    parts.addAll(resolveActingParameterIds(valueProviderModel, parameterModelsInformation));

    String id = resolveDslTag(containerComponent).orElse(getSourceElementName(containerComponent));
    return of(aValueProviderCacheId(fromElementWithName(id).withHashValueFrom(id).containing(parts)));
  }

  private Optional<ValueProviderCacheId> resolveForComponentModel(DslElementModel<?> containerComponent,
                                                                  ValueProviderModel valueProviderModel,
                                                                  Map<String, ParameterModelInformation> parameterModelsInformation) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    parts.add(resolveValueProviderId(valueProviderModel));
    parts.addAll(resolveActingParameterIds(valueProviderModel, parameterModelsInformation));
    parts.addAll(resolveIdForInjectedElements(containerComponent, valueProviderModel));

    String id = resolveDslTag(containerComponent).orElse(getSourceElementName(containerComponent));
    return of(aValueProviderCacheId(fromElementWithName(id).withHashValueFrom(id).containing(parts)));
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
        resolveIdForInjectedElement(configDslElementModel.get())
            .ifPresent(id -> injectableIds.add(aValueProviderCacheId(fromElementWithName("config: ").containing(id))));
      }
      if (valueProviderModel.requiresConnection()) {
        configDslElementModel.get().getContainedElements().stream()
            .filter(nested -> nested.getModel() instanceof ConnectionProviderModel).forEach(
                                                                                            connectionProvider -> resolveIdForInjectedElement(connectionProvider)
                                                                                                .ifPresent(id -> injectableIds
                                                                                                    .add(aValueProviderCacheId(fromElementWithName("connection: ")
                                                                                                        .containing(id)))));
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
    String sourceElementName = sourceElementNameFromSimpleValue(injectedElement);
    return of(aValueProviderCacheId(fromElementWithName(sourceElementName).withHashValueFrom(sourceElementName)
        .containing(parts)));
  }

  private ValueProviderCacheId resolveValueProviderId(ValueProviderModel valueProviderModel) {
    return aValueProviderCacheId(fromElementWithName("valueProvider: " + valueProviderModel.getProviderName())
        .withHashValueFrom(valueProviderModel.getProviderName()));
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

  private Optional<String> resolveDslTag(DslElementModel<?> elementModel) {
    return elementModel.getIdentifier().map(Object::toString);
  }

  private Optional<ValueProviderCacheId> resolveParameterId(DslElementModel<?> parameterModel) {
    return parameterModel
        .getValue()
        .map(v -> resolveSimpleValue(parameterModel, locator)
            .map(either -> either.reduce(
                                         l -> resolveIdRecursively(l).orElse(null),
                                         r -> aValueProviderCacheId(fromElementWithName(sourceElementNameFromSimpleValue(parameterModel))
                                             .withHashValueFrom(v)))))
        .orElse(resolveIdRecursively(parameterModel));
  }

  private Optional<ValueProviderCacheId> resolveIdRecursively(DslElementModel<?> element) {
    if (element.getValue().isPresent()) {
      return empty();
    } else {
      String id = resolveDslTag(element).orElse(getSourceElementName(element));
      ValueProviderCacheId.ValueProviderCacheIdBuilder vpIdBuilder = fromElementWithName(id).withHashValueFrom(id);
      element.getContainedElements().stream().map(this::resolveParameterId)
          .forEach(resolvedId -> resolvedId.ifPresent(vpIdBuilder::containing));
      return of(aValueProviderCacheId(vpIdBuilder));
    }
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
