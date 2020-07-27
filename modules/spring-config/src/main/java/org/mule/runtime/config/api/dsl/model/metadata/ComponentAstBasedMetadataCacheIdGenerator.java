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
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.util.NameUtils.hyphenize;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedIdHelper.getModelNameAst;
import static org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedIdHelper.getSourceElementName;
import static org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedIdHelper.resolveConfigName;
import static org.mule.runtime.config.api.dsl.model.metadata.ComponentBasedIdHelper.sourceElementNameFromSimpleValue;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
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

          resolveGlobalElement(elementModel)
              .ifPresent(keyParts::add);

          return new MetadataCacheId(keyParts, getSourceElementName(elementModel));
        })
        .orElseGet(() -> {
          Optional<MetadataCacheId> configId = resolveConfigId(elementModel);
          if (configId.isPresent()) {
            keyParts.add(configId.get());
            resolveCategoryId(elementModel)
                .ifPresent(keyParts::add);
            return new MetadataCacheId(keyParts, getSourceElementName(elementModel));
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
      resolveConfigId(component).ifPresent(keyParts::add);

      typeInformation.getResolverCategory()
          .ifPresent(resolverCategory -> keyParts
              .add(createCategoryMetadataCacheId(resolverCategory)));

      typeInformation.getResolverName()
          .ifPresent(resolverName -> keyParts.add(createResolverMetadataCacheId(resolverName)));

      component.getModel(ComponentModel.class)
          .flatMap(cmpModel -> resolveMetadataKeyParts(component, cmpModel,
                                                       typeInformation.shouldIncludeConfiguredMetadataKeys()))

          .ifPresent(keyParts::add);
    } else {
      keyParts.add(resolveDslTagId(component));

      component.getModel(ConfigurationModel.class)
          .flatMap(cfgModel -> resolveGlobalElement(component))
          .ifPresent(keyParts::add);
    }

    keyParts.add(typeInformation.getComponentTypeMetadataCacheId());

    return of(new MetadataCacheId(keyParts,
                                  typeInformation.getComponentTypeMetadataCacheId().getSourceElementName()
                                      .map(sourceElementName -> format("(%s):(%s)", getSourceElementName(component),
                                                                       sourceElementName))
                                      .orElse(format("(%s):(%s)", getSourceElementName(component), "Unknown Type"))));
  }

  private Optional<MetadataCacheId> doResolve(ComponentAst elementModel) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    resolveConfigId(elementModel)
        .ifPresent(keyParts::add);

    resolveCategoryId(elementModel)
        .ifPresent(keyParts::add);

    keyParts.add(resolveDslTagId(elementModel));

    elementModel.getModel(ComponentModel.class)
        .map(model -> resolveMetadataKeyParts(elementModel, model, true))
        .orElseGet(() -> resolveGlobalElement(elementModel))
        .ifPresent(keyParts::add);

    return of(new MetadataCacheId(keyParts, getSourceElementName(elementModel)));
  }

  private MetadataCacheId resolveDslTagId(ComponentAst elementModel) {
    final ComponentIdentifier id = elementModel.getIdentifier();
    return new MetadataCacheId(id.hashCode(), id.toString());
  }

  private Optional<MetadataCacheId> resolveConfigId(ComponentAst elementModel) {
    return resolveConfigName(elementModel).flatMap(this::getHashedGlobal);
  }

  private Optional<MetadataCacheId> resolveGlobalElement(ComponentAst elementModel) {
    List<String> parameterNamesRequiredForMetadata = parameterNamesRequiredForMetadataCacheId(elementModel);

    final List<String> paramsAsChildrenNames = elementModel.getModel(ParameterizedModel.class)
        .map(pmz -> pmz.getAllParameterModels()
            .stream()
            .map(pm -> hyphenize(pm.getName()))
            .collect(toList()))
        .orElse(emptyList());

    List<MetadataCacheId> parts = Stream.concat(
                                                elementModel.directChildrenStream()
                                                    // TODO MULE-17711 Remove this filter
                                                    .filter(c -> !paramsAsChildrenNames.contains(c.getIdentifier().getName())
                                                        && !c.getModel(ParameterModel.class).isPresent())
                                                    .map(c -> doResolve(c))
                                                    .filter(Optional::isPresent)
                                                    .map(Optional::get),
                                                elementModel.getModel(ParameterizedModel.class)
                                                    .map(pmz -> elementModel.getParameters()
                                                        .stream()
                                                        .filter(p -> parameterNamesRequiredForMetadata
                                                            .contains((p.getModel()).getName()))
                                                        .map(p -> resolveKeyFromSimpleValue(elementModel, p)))
                                                    .orElse(Stream.empty()))
        .collect(toList());

    if (parts.isEmpty()) {
      return empty();
    }

    return of(new MetadataCacheId(parts, getModelNameAst(elementModel).orElse(null)));
  }

  private List<String> parameterNamesRequiredForMetadataCacheId(ComponentAst component) {
    return component.getModel(EnrichableModel.class)
        .flatMap(model -> model.getModelProperty(RequiredForMetadataModelProperty.class)
            .map(RequiredForMetadataModelProperty::getRequiredParameters))
        .orElse(emptyList());
  }

  private Optional<MetadataCacheId> resolveMetadataKeyParts(ComponentAst elementModel,
                                                            ComponentModel componentModel,
                                                            boolean resolveAllKeys) {
    boolean isPartialFetching = componentModel.getModelProperty(MetadataResolverFactoryModelProperty.class)
        .map(mp -> mp.getMetadataResolverFactory().getKeyResolver()).map(kr -> kr instanceof PartialTypeKeysResolver)
        .orElse(false);

    if (isPartialFetching || resolveAllKeys) {
      List<MetadataCacheId> keyParts = elementModel.getParameters()
          .stream()
          .filter(p -> p.getModel().getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
          .map(p -> resolveKeyFromSimpleValue(elementModel, p))
          .collect(toList());
      return keyParts.isEmpty() ? empty() : of(new MetadataCacheId(keyParts, "metadataKey"));
    }

    return empty();
  }

  private MetadataCacheId resolveKeyFromSimpleValue(ComponentAst elementModel, ComponentParameterAst param) {
    final String sourceElementName = sourceElementNameFromSimpleValue(elementModel, param);

    return param.getValue().reduce(v -> {
      return new MetadataCacheId(v.hashCode(), sourceElementName);
    }, v -> {
      Reference<MetadataCacheId> reference = new Reference<>();
      final MetadataCacheId valuePart = new MetadataCacheId(v.hashCode(), sourceElementName);

      if (v instanceof ComponentAst) {
        param.getModel().getType().accept(new MetadataTypeVisitor() {

          @Override
          public void visitArrayType(ArrayType arrayType) {
            getHashedGlobal(param.getRawValue()).ifPresent(reference::set);
          }

          @Override
          public void visitObject(ObjectType objectType) {
            boolean canBeGlobal = objectType.getAnnotation(TypeDslAnnotation.class)
                .map(TypeDslAnnotation::allowsTopLevelDefinition).orElse(false);

            if (canBeGlobal) {
              getHashedGlobal(param.getRawValue()).ifPresent(reference::set);
            }
          }
        });
      } else {
        final ParameterModel paramModel = param.getModel();
        paramModel.getType().accept(new MetadataTypeVisitor() {

          @Override
          public void visitString(StringType stringType) {
            if (!paramModel.getAllowedStereotypes().isEmpty()) {
              getHashedGlobal(v.toString()).ifPresent(reference::set);
            }
          }

          @Override
          public void visitArrayType(ArrayType arrayType) {
            if (paramModel.getDslConfiguration().allowsReferences() && v instanceof String) {
              getHashedGlobal(v.toString()).ifPresent(reference::set);
            }
          }

          @Override
          public void visitObject(ObjectType objectType) {
            if (paramModel.getDslConfiguration().allowsReferences()) {
              getHashedGlobal(v.toString()).ifPresent(reference::set);
            }
          }

        });

      }

      return reference.get() == null ? valuePart : reference.get();
    });
  }

  private MetadataCacheId createCategoryMetadataCacheId(String category) {
    return new MetadataCacheId(category.hashCode(), "category: " + category);
  }

  private MetadataCacheId createResolverMetadataCacheId(String resolverName) {
    return new MetadataCacheId(resolverName.hashCode(), "resolver: " + resolverName);
  }

  private Optional<MetadataCacheId> getHashedGlobal(String name) {
    if (!isBlank(name)) {
      return locator.get(Location.builder().globalName(name).build())
          .map(global -> doResolve(global).orElse(null));
    }
    return empty();
  }
}
