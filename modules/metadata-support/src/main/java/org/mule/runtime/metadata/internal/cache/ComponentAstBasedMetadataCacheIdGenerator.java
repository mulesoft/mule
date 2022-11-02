/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.getModelNameAst;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveConfigName;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.sourceElementName;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.parameterNamesRequiredForMetadataCacheId;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveDslTagId;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveKeyFromSimpleValue;
import static org.mule.runtime.metadata.internal.cache.ComponentBasedIdHelper.resolveMetadataKeyParts;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.metadata.api.locator.ComponentLocator;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.api.cache.MetadataCacheIdGenerator;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.metadata.internal.types.AttributesMetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.InputMetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.KeysMetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.MetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.OutputMetadataResolutionTypeInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * A {@link ComponentAst} based implementation of a {@link MetadataCacheIdGenerator}
 *
 * @since 4.4
 */
public class ComponentAstBasedMetadataCacheIdGenerator implements MetadataCacheIdGenerator<ComponentAst> {

  private final ComponentLocator<ComponentAst> locator;

  public ComponentAstBasedMetadataCacheIdGenerator(ComponentLocator<ComponentAst> locator) {
    this.locator = locator;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentOutputMetadata(ComponentAst component) {
    return component.getModel(HasOutputModel.class)
        .flatMap(hom -> doResolveType(component, new OutputMetadataResolutionTypeInformation(component)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(ComponentAst component) {
    return component.getModel(HasOutputModel.class)
        .flatMap(hom -> doResolveType(component, new AttributesMetadataResolutionTypeInformation(component)));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentAst component, String parameterName) {
    checkArgument(component.getModel(ParameterizedModel.class).isPresent(),
                  () -> "Cannot generate an Input Cache Key for component '" + component.toString() + "' with no parameters");
    checkArgument(component.getModel(ParameterizedModel.class).get().getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(parameterName)),
                  () -> "Cannot generate an Input Cache Key for component '" + component.toString()
                      + "' since it does not have a parameter named "
                      + parameterName);
    return doResolveType(component, new InputMetadataResolutionTypeInformation(component, parameterName));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForComponentMetadata(ComponentAst elementModel) {
    return doResolve(elementModel);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(ComponentAst elementModel) {
    return doResolveType(elementModel, new KeysMetadataResolutionTypeInformation(elementModel));
  }

  @Override
  public Optional<MetadataCacheId> getIdForGlobalMetadata(ComponentAst elementModel) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    return of(elementModel.getModel(ConfigurationModel.class)
        .map(cfgModel -> {
          keyParts.add(resolveDslTagId(elementModel));

          resolveGlobalElement(elementModel).ifPresent(keyParts::add);

          return new MetadataCacheId(keyParts, sourceElementName(elementModel));
        })
        .orElseGet(() -> {
          Optional<MetadataCacheId> configId = resolveConfigId(elementModel);
          if (configId.isPresent()) {
            keyParts.add(configId.get());
            resolveCategoryId(elementModel).ifPresent(keyParts::add);
            return new MetadataCacheId(keyParts, sourceElementName(elementModel));
          }

          return resolveDslTagId(elementModel);
        }));
  }

  private Optional<MetadataCacheId> resolveCategoryId(ComponentAst elementModel) {
    return elementModel.getModel(ComponentModel.class)
        .flatMap(model -> model.getModelProperty(MetadataKeyIdModelProperty.class))
        .map(mp -> mp.getCategoryName().orElse(null))
        .map(this::createCategoryMetadataCacheId);
  }

  private Optional<MetadataCacheId> doResolveType(ComponentAst component,
                                                  MetadataResolutionTypeInformation typeInformation) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    if (typeInformation.isDynamicType()) {
      resolveDslTagNamespace(component).ifPresent(keyParts::add);

      resolveConfigId(component).ifPresent(keyParts::add);

      typeInformation.getResolverCategory()
          .ifPresent(resolverCategory -> keyParts.add(createCategoryMetadataCacheId(resolverCategory)));

      typeInformation.getResolverName().ifPresent(resolverName -> keyParts.add(createResolverMetadataCacheId(resolverName)));

      keyParts.add(typeInformation.getComponentTypeMetadataCacheId());

      component.getModel(ComponentModel.class)
          .flatMap(cmpModel -> resolveMetadataKeyParts(component, cmpModel, typeInformation.shouldIncludeConfiguredMetadataKeys(),
                                                       this::getHashedGlobal))

          .ifPresent(keyParts::add);
    } else {
      keyParts.add(resolveDslTagId(component));

      component.getModel(ConfigurationModel.class)
          .flatMap(cfgModel -> resolveGlobalElement(component))
          .ifPresent(keyParts::add);

      keyParts.add(typeInformation.getComponentTypeMetadataCacheId());
    }

    return of(new MetadataCacheId(keyParts,
                                  typeInformation.getComponentTypeMetadataCacheId().getSourceElementName()
                                      .map(sourceElementName -> format("(%s):(%s)", sourceElementName(component),
                                                                       sourceElementName))
                                      .orElse(format("(%s):(%s)", sourceElementName(component), "Unknown Type"))));
  }

  private Optional<MetadataCacheId> resolveDslTagNamespace(ComponentAst elementModel) {
    String namespace = elementModel.getIdentifier().getNamespace();
    return of(new MetadataCacheId(namespace.toLowerCase().hashCode(), namespace));
  }

  private Optional<MetadataCacheId> doResolve(ComponentAst elementModel) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    resolveConfigId(elementModel).ifPresent(keyParts::add);

    resolveCategoryId(elementModel).ifPresent(keyParts::add);

    keyParts.add(resolveDslTagId(elementModel));

    elementModel.getModel(ComponentModel.class)
        .map(model -> resolveMetadataKeyParts(elementModel, model, true, this::getHashedGlobal))
        .orElseGet(() -> resolveGlobalElement(elementModel))
        .ifPresent(keyParts::add);

    return of(new MetadataCacheId(keyParts, sourceElementName(elementModel)));
  }

  private Optional<MetadataCacheId> resolveConfigId(ComponentAst elementModel) {
    return resolveConfigName(elementModel).flatMap(this::getHashedGlobal);
  }

  private Optional<MetadataCacheId> resolveGlobalElement(ComponentAst elementModel) {
    List<String> parameterNamesRequiredForMetadata = parameterNamesRequiredForMetadataCacheId(elementModel);

    List<MetadataCacheId> parts = Stream.concat(elementModel.directChildrenStream()
        .map(this::doResolve)
        .filter(Optional::isPresent)
        .map(Optional::get), elementModel.getModel(ParameterizedModel.class)
            .map(pmz -> elementModel.getParameters()
                .stream()
                .filter(p -> p.getValue().getValue().isPresent())
                .filter(p -> parameterNamesRequiredForMetadata
                    .contains((p.getModel()).getName()))
                .map(p -> resolveKeyFromSimpleValue(elementModel, p, this::getHashedGlobal)))
            .orElse(Stream.empty()))
        .collect(toList());

    if (parts.isEmpty()) {
      return empty();
    }

    return of(new MetadataCacheId(parts, getModelNameAst(elementModel).orElse(null)));
  }

  private MetadataCacheId createCategoryMetadataCacheId(String category) {
    return new MetadataCacheId(category.hashCode(), "category: " + category);
  }

  private MetadataCacheId createResolverMetadataCacheId(String resolverName) {
    return new MetadataCacheId(resolverName.hashCode(), "resolver: " + resolverName);
  }

  private Optional<MetadataCacheId> getHashedGlobal(String name) {
    if (isBlank(name)) {
      return empty();
    }
    return locator.get(Location.builder().globalName(name).build()).flatMap(this::doResolve);
  }

}
