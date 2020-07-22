/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.aValueProviderCacheId;
import static org.mule.runtime.core.internal.value.cache.ValueProviderCacheId.ValueProviderCacheIdBuilder.fromElementWithName;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.Typed;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.parameter.ValueProviderModel;
import org.mule.runtime.config.api.dsl.model.metadata.context.ValueProviderCacheIdGeneratorContext;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheId;
import org.mule.runtime.core.internal.value.cache.ValueProviderCacheIdGenerator;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;

import com.google.common.base.Objects;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public class ContextBasedValueProviderCacheIdGenerator
    implements ValueProviderCacheIdGenerator<ValueProviderCacheIdGeneratorContext<?>> {

  @Override
  public Optional<ValueProviderCacheId> getIdForResolvedValues(ValueProviderCacheIdGeneratorContext context,
                                                               String parameterName) {
    return ifContainsParameter(context, parameterName)
        .flatMap(ParameterModel::getValueProviderModel)
        .flatMap(valueProviderModel -> resolveId(context, valueProviderModel));
  }

  private Optional<ParameterModel> ifContainsParameter(ValueProviderCacheIdGeneratorContext context, String parameterName) {
    return context.getOwnerModel()
        .getAllParameterModels()
        .stream()
        .filter(p -> Objects.equal(parameterName, p.getName()))
        .findAny();
  }

  private Optional<ValueProviderCacheId> resolveId(ValueProviderCacheIdGeneratorContext<?> context,
                                                   ValueProviderModel valueProviderModel) {
    if (context.isForComponent()) {
      return resolveForComponentModel(context, valueProviderModel);
    } else {
      return resolveForGlobalElement(context, valueProviderModel);
    }
  }

  private Optional<ValueProviderCacheId> resolveForGlobalElement(ValueProviderCacheIdGeneratorContext<?> context,
                                                                 ValueProviderModel valueProviderModel) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    parts.add(resolveValueProviderId(valueProviderModel));
    parts.addAll(resolveActingParameterIds(context, valueProviderModel));

    String id = getSourceElementName(context.getOwnerId(), context.getOwnerModel());
    return of(aValueProviderCacheId(fromElementWithName(id).withHashValueFrom(id).containing(parts)));
  }

  private Optional<ValueProviderCacheId> resolveForComponentModel(ValueProviderCacheIdGeneratorContext<?> context,
                                                                  ValueProviderModel valueProviderModel) {
    List<ValueProviderCacheId> parts = new LinkedList<>();

    parts.add(resolveValueProviderId(valueProviderModel));
    parts.addAll(resolveActingParameterIds(context, valueProviderModel));
    parts.addAll(resolveIdForInjectedElements(context, valueProviderModel));

    String id = getSourceElementName(context.getOwnerId(), context.getOwnerModel());
    return of(aValueProviderCacheId(fromElementWithName(id).withHashValueFrom(id).containing(parts)));
  }

  private List<ValueProviderCacheId> resolveIdForInjectedElements(ValueProviderCacheIdGeneratorContext<?> context,
                                                                  ValueProviderModel valueProviderModel) {
    if (!valueProviderModel.requiresConfiguration() && !valueProviderModel.requiresConnection()) {
      return emptyList();
    }

    List<ValueProviderCacheId> injectableIds = new LinkedList<>();

    if (valueProviderModel.requiresConfiguration()) {
      context.getConfigContext().ifPresent(
                                           configContext -> resolveIdForInjectedElement(configContext)
                                               .ifPresent(id -> injectableIds
                                                   .add(aValueProviderCacheId(fromElementWithName("config: ").containing(id)))));
    }

    if (valueProviderModel.requiresConnection()) {
      context.getConnectionContext()
          .ifPresent(connectionContext -> resolveIdForInjectedElement(connectionContext)
              .ifPresent(id -> injectableIds.add(aValueProviderCacheId(fromElementWithName("connection: ").containing(id)))));
    }

    return injectableIds;
  }


  private Optional<ValueProviderCacheId> resolveIdForInjectedElement(ValueProviderCacheIdGeneratorContext<?> injectedElementContext) {
    ParameterizedModel injectedElementModel = injectedElementContext.getOwnerModel();
    if (injectedElementModel instanceof EnrichableModel) {
      EnrichableModel enrichableModel = (EnrichableModel) injectedElementModel;
      List<String> parametersRequiredForMetadata =
          enrichableModel
              .getModelProperty(RequiredForMetadataModelProperty.class)
              .map(RequiredForMetadataModelProperty::getRequiredParameters)
              .orElse(emptyList());

      List<ValueProviderCacheId> parts = parametersRequiredForMetadata
          .stream()
          .filter(p -> injectedElementContext.getParameters().containsKey(p))
          .map(requiredParameter -> resolveParameterId(injectedElementContext,
                                                       injectedElementContext.getParameters().get(requiredParameter)))
          .collect(toList());

      if (parts.isEmpty()) {
        return empty();
      }

      String sourceElementName = sourceElementNameFromSimpleValue(injectedElementContext.getOwnerId(),
                                                                  injectedElementContext.getOwnerModel());

      return of(aValueProviderCacheId(fromElementWithName(sourceElementName).withHashValueFrom(sourceElementName)
          .containing(parts)));
    }

    return empty();
  }

  private ValueProviderCacheId resolveValueProviderId(ValueProviderModel valueProviderModel) {
    return aValueProviderCacheId(fromElementWithName("valueProvider: " + valueProviderModel.getProviderName())
        .withHashValueFrom(valueProviderModel.getProviderName()));
  }

  private List<ValueProviderCacheId> resolveActingParameterIds(ValueProviderCacheIdGeneratorContext<?> context,
                                                               ValueProviderModel valueProviderModel) {
    return valueProviderModel.getActingParameters()
        .stream()
        .filter(k -> context.getParameters().containsKey(k))
        .map(ap -> resolveParameterId(context, context.getParameters().get(ap)))
        .collect(toList());
  }

  private ValueProviderCacheId resolveParameterId(ValueProviderCacheIdGeneratorContext context,
                                                  ValueProviderCacheIdGeneratorContext.ParameterInfo parameterInfo) {
    return aValueProviderCacheId(fromElementWithName("param:"
        + sourceElementNameFromSimpleValue(context.getOwnerId(), parameterInfo.getName()))
            .withHashValue(parameterInfo.getHashValue()));
  }

  private String getSourceElementName(ComponentIdentifier identifier, ParameterizedModel model) {
    return identifier + getModelNameAst(model).orElse("[" + model.getName() + "]");
  }

  private Optional<String> getModelNameAst(ParameterizedModel model) {
    Optional<String> namedObjectName = ifInstanceOf(model, NamedObject.class, NamedObject::getName);
    if (namedObjectName.isPresent()) {
      return namedObjectName;
    }

    return ifInstanceOf(model, Typed.class, t -> ExtensionMetadataTypeUtils.getId(t.getType()).toString());
  }

  private String sourceElementNameFromSimpleValue(ComponentIdentifier identifier, ParameterizedModel elementModel) {
    return getModelNameAst(elementModel)
        .map(modelName -> identifier.getNamespace() + ":" + modelName)
        .orElseGet(identifier::toString);
  }

  private String sourceElementNameFromSimpleValue(ComponentIdentifier ownerIdentifier, String parameter) {
    return ownerIdentifier.getNamespace() + ":" + parameter;
  }

  private <T, R> Optional<R> ifInstanceOf(Object instance, Class<T> clazz, Function<T, R> mapper) {
    if (clazz.isAssignableFrom(instance.getClass())) {
      return ofNullable(mapper.apply(clazz.cast(instance)));
    }
    return empty();
  }
}
