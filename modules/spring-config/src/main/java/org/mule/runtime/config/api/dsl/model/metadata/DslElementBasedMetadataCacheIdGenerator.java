/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.getGlobalElement;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.getModelName;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.getSourceElementName;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.resolveConfigName;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.resolveSimpleValue;
import static org.mule.runtime.config.api.dsl.model.metadata.DslElementIdHelper.sourceElementNameFromSimpleValue;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.metadata.types.AttributesMetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.InputMetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.KeysMetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.MetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.OutputMetadataResolutionTypeInformation;
import org.mule.runtime.core.internal.locator.ComponentLocator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link DslElementModel} based implementation of a {@link MetadataCacheIdGenerator}
 *
 * @since 4.1.4, 4.2.0
 */
public class DslElementBasedMetadataCacheIdGenerator implements MetadataCacheIdGenerator<DslElementModel<?>> {

  private static final Logger LOGGER = LoggerFactory.getLogger(DslElementBasedMetadataCacheIdGenerator.class);
  private final ComponentLocator<DslElementModel<?>> locator;

  public DslElementBasedMetadataCacheIdGenerator(ComponentLocator<DslElementModel<?>> locator) {
    this.locator = locator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentOutputMetadata(DslElementModel<?> component) {
    if (component.getModel() == null || !(component.getModel() instanceof HasOutputModel)) {
      return empty();
    }
    return doResolveType(component, new OutputMetadataResolutionTypeInformation(component));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(DslElementModel<?> component) {
    if (component.getModel() == null || !(component.getModel() instanceof HasOutputModel)) {
      return empty();
    }
    return doResolveType(component, new AttributesMetadataResolutionTypeInformation(component));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(DslElementModel<?> component, String parameterName) {
    checkArgument(component.getModel() != null, "Cannot generate an Input Cache Key for a 'null' component");
    checkArgument(component.getModel() instanceof ParameterizedModel,
                  "Cannot generate an Input Cache Key for a component with no parameters");
    checkArgument(((ParameterizedModel) component.getModel()).getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(parameterName)),
                  "Cannot generate an Input Cache Key for the component since it does not have a parameter named "
                      + parameterName);
    return doResolveType(component, new InputMetadataResolutionTypeInformation(component, parameterName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentMetadata(DslElementModel<?> elementModel) {
    return doResolve(elementModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(DslElementModel<?> elementModel) {
    return doResolveType(elementModel, new KeysMetadataResolutionTypeInformation(elementModel));
  }

  @Override
  public Optional<MetadataCacheId> getIdForGlobalMetadata(DslElementModel<?> elementModel) {
    List<MetadataCacheId> keyParts = new ArrayList<>();
    if (elementModel.getModel() instanceof ConfigurationModel) {
      resolveDslTagId(elementModel)
          .ifPresent(keyParts::add);

      resolveGlobalElement(elementModel)
          .ifPresent(keyParts::add);

      return of(new MetadataCacheId(keyParts, getSourceElementName(elementModel)));
    }

    Optional<MetadataCacheId> configId = resolveConfigId(elementModel);
    if (configId.isPresent()) {
      keyParts.add(configId.get());
      resolveCategoryId(elementModel)
          .ifPresent(keyParts::add);
      return of(new MetadataCacheId(keyParts, getSourceElementName(elementModel)));
    }

    return resolveDslTagId(elementModel);
  }

  private Optional<MetadataCacheId> resolveCategoryId(DslElementModel<?> elementModel) {
    if (!(elementModel.getModel() instanceof ComponentModel)) {
      return empty();
    }

    return ((ComponentModel) elementModel.getModel()).getModelProperty(MetadataKeyIdModelProperty.class)
        .map(mp -> mp.getCategoryName().orElse(null))
        .map(this::createCategoryMetadataCacheId);
  }

  private Optional<MetadataCacheId> doResolveType(DslElementModel<?> component,
                                                  MetadataResolutionTypeInformation typeInformation) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    if (typeInformation.isDynamicType()) {
      resolveConfigId(component).ifPresent(keyParts::add);

      typeInformation.getResolverCategory()
          .ifPresent(resolverCategory -> keyParts
              .add(createCategoryMetadataCacheId(resolverCategory)));

      typeInformation.getResolverName()
          .ifPresent(resolverName -> keyParts.add(createResolverMetadataCacheId(resolverName)));

      Object model = component.getModel();
      if (model instanceof ComponentModel) {
        resolveMetadataKeyParts(component, (ComponentModel) model, typeInformation.shouldIncludeConfiguredMetadataKeys())
            .ifPresent(keyParts::add);
      }
    } else {
      resolveDslTagId(component).ifPresent(keyParts::add);
      if (component.getModel() instanceof ConfigurationModel) {
        resolveGlobalElement(component)
            .ifPresent(keyParts::add);
      }
    }

    keyParts.add(typeInformation.getComponentTypeMetadataCacheId());

    return of(new MetadataCacheId(keyParts,
                                  typeInformation.getComponentTypeMetadataCacheId().getSourceElementName()
                                      .map(sourceElementName -> format("(%s):(%s)", getSourceElementName(component),
                                                                       sourceElementName))
                                      .orElse(format("(%s):(%s)", getSourceElementName(component), "Unknown Type"))));
  }

  private Optional<MetadataCacheId> doResolve(DslElementModel<?> elementModel) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    resolveConfigId(elementModel)
        .ifPresent(keyParts::add);

    resolveCategoryId(elementModel)
        .ifPresent(keyParts::add);

    resolveDslTagId(elementModel)
        .ifPresent(keyParts::add);

    Object model = elementModel.getModel();
    if (model instanceof ComponentModel) {
      resolveMetadataKeyParts(elementModel, (ComponentModel) model, true).ifPresent(keyParts::add);
    } else {
      resolveGlobalElement(elementModel).ifPresent(keyParts::add);
    }

    return of(new MetadataCacheId(keyParts, getSourceElementName(elementModel)));
  }

  private Optional<MetadataCacheId> resolveDslTagId(DslElementModel<?> elementModel) {
    return elementModel.getIdentifier()
        .map(id -> new MetadataCacheId(id.hashCode(), id.toString()));
  }

  private Optional<MetadataCacheId> resolveConfigId(DslElementModel<?> elementModel) {
    return resolveConfigName(elementModel).flatMap(this::getHashedGlobal);
  }

  private Optional<MetadataCacheId> resolveGlobalElement(DslElementModel<?> elementModel) {
    List<MetadataCacheId> parts = new ArrayList<>();
    List<String> parameterNamesRequiredForMetadata = parameterNamesRequiredForMetadataCacheId(elementModel.getModel());
    elementModel.getContainedElements().stream()
        .filter(containedElement -> containedElement.getModel() != null)
        .filter(containedElement -> isRequiredForMetadata(parameterNamesRequiredForMetadata, containedElement.getModel()))
        .forEach(containedElement -> {
          if (containedElement.getValue().isPresent()) {
            resolveKeyFromSimpleValue(containedElement).ifPresent(parts::add);
          } else {
            getIdForComponentMetadata(containedElement).ifPresent(parts::add);
          }
        });

    if (parts.isEmpty()) {
      return empty();
    }

    return of(new MetadataCacheId(parts, getModelName(elementModel.getModel()).orElse(null)));
  }

  private boolean isRequiredForMetadata(List<String> parameterNamesRequiredForMetadataCacheId, Object model) {
    if (model instanceof ParameterModel) {
      return parameterNamesRequiredForMetadataCacheId.contains(((ParameterModel) model).getName());
    } else {
      return true;
    }
  }

  private List<String> parameterNamesRequiredForMetadataCacheId(Object model) {
    if (model instanceof EnrichableModel) {
      return ((EnrichableModel) model).getModelProperty(RequiredForMetadataModelProperty.class)
          .map(RequiredForMetadataModelProperty::getRequiredParameters).orElse(emptyList());
    }
    return emptyList();
  }

  private Optional<MetadataCacheId> resolveMetadataKeyParts(DslElementModel<?> elementModel,
                                                            ComponentModel componentModel,
                                                            boolean resolveAllKeys) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    boolean isPartialFetching = componentModel.getModelProperty(MetadataResolverFactoryModelProperty.class)
        .map(mp -> mp.getMetadataResolverFactory().getKeyResolver()).map(kr -> kr instanceof PartialTypeKeysResolver)
        .orElse(false);

    if (isPartialFetching || resolveAllKeys) {
      componentModel.getAllParameterModels().stream()
          .filter(p -> p.getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
          .map(metadataKeyPart -> elementModel.findElement(metadataKeyPart.getName()))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .filter(partElement -> partElement.getValue().isPresent())
          .forEach(partElement -> resolveKeyFromSimpleValue(partElement).ifPresent(keyParts::add));
    }

    return keyParts.isEmpty() ? empty() : of(new MetadataCacheId(keyParts, "metadataKey"));
  }

  private Optional<MetadataCacheId> resolveKeyFromSimpleValue(DslElementModel<?> element) {
    return resolveSimpleValue(element, locator)
        .flatMap(either -> either.reduce(this::getIdForComponentMetadata,
                                         r -> of(new MetadataCacheId(r.hashCode(), sourceElementNameFromSimpleValue(element)))));
  }

  private MetadataCacheId createCategoryMetadataCacheId(String category) {
    return new MetadataCacheId(category.hashCode(), "category: " + category);
  }

  private MetadataCacheId createResolverMetadataCacheId(String resolverName) {
    return new MetadataCacheId(resolverName.hashCode(), "resolver: " + resolverName);
  }

  private Optional<MetadataCacheId> getHashedGlobal(String name) {
    return getGlobalElement(name, locator).flatMap(this::getIdForComponentMetadata);
  }
}
