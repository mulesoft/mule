/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.api.dsl.model.metadata;

import static java.lang.String.format;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.api.util.StringUtils.isBlank;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.config.api.dsl.model.DslElementModel;
import org.mule.runtime.config.api.dsl.model.metadata.types.AttributesMetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.InputMetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.MetadataResolutionTypeInformation;
import org.mule.runtime.config.api.dsl.model.metadata.types.OutputMetadataResolutionTypeInformation;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheId;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGenerator;
import org.mule.runtime.core.internal.metadata.cache.MetadataCacheIdGeneratorFactory.ComponentLocator;
import org.mule.runtime.dsl.api.component.config.ComponentConfiguration;
import org.mule.runtime.extension.api.declaration.type.annotation.TypeDslAnnotation;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils;
import org.mule.runtime.module.extension.internal.loader.java.property.MetadataResolverFactoryModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

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
    return doResolve(elementModel, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(DslElementModel<?> elementModel) {
    return doResolve(elementModel, false);
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
        .map(name -> new MetadataCacheId(name.hashCode(), "category:" + name));
  }

  private Optional<MetadataCacheId> doResolveType(DslElementModel<?> component,
                                                  MetadataResolutionTypeInformation typeInformation) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    if (typeInformation.isDynamicType()) {
      resolveConfigId(component).ifPresent(keyParts::add);

      typeInformation.getResolverCategory()
          .ifPresent(resolverCategory -> keyParts
              .add(new MetadataCacheId(resolverCategory.hashCode(), "category:" + resolverCategory)));

      typeInformation.getResolverName()
          .ifPresent(resolverName -> keyParts.add(new MetadataCacheId(resolverName.hashCode(), "resolver:" + resolverName)));

      Object model = component.getModel();
      if (model instanceof ComponentModel) {
        resolveMetadataKeyParts(component, (ComponentModel) model, true).ifPresent(keyParts::add);
      }
    } else {
      resolveDslTagId(component).ifPresent(keyParts::add);
    }

    keyParts.add(typeInformation.getComponentTypeMetadataCacheId());

    return of(new MetadataCacheId(keyParts,
                                  typeInformation.getComponentTypeMetadataCacheId().getSourceElementName()
                                      .map(sourceElementName -> format("(%s):(%s)", getSourceElementName(component),
                                                                       sourceElementName))
                                      .orElse(format("(%s):(%s)", getSourceElementName(component), "Unknown Type"))));
  }

  private Optional<MetadataCacheId> doResolve(DslElementModel<?> elementModel,
                                              boolean isComponentResolution) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    resolveConfigId(elementModel)
        .ifPresent(keyParts::add);

    resolveCategoryId(elementModel)
        .ifPresent(keyParts::add);

    if (isComponentResolution) {
      resolveDslTagId(elementModel)
          .ifPresent(keyParts::add);
    }

    Object model = elementModel.getModel();
    if (model instanceof ComponentModel) {
      resolveMetadataKeyParts(elementModel, (ComponentModel) model, isComponentResolution).ifPresent(keyParts::add);
    } else {
      resolveGlobalElement(elementModel).ifPresent(keyParts::add);
    }

    return of(new MetadataCacheId(keyParts, getSourceElementName(elementModel)));
  }

  private Optional<MetadataCacheId> resolveDslTagId(DslElementModel<?> elementModel) {
    return elementModel.getIdentifier()
        .map(id -> new MetadataCacheId(id.hashCode(), id.toString()));
  }

  private String getSourceElementName(DslElementModel<?> elementModel) {
    return elementModel.getDsl().getPrefix() + ":" +
        getModelName(elementModel.getModel()).orElse(elementModel.getDsl().getElementName()) +
        elementModel.getConfiguration()
            .map(c -> c.getParameters().get("name")).filter(Objects::nonNull)
            .map(n -> "[" + n + "]").orElse("");
  }

  private Optional<MetadataCacheId> resolveConfigId(DslElementModel<?> elementModel) {
    // TODO Migrate to Stereotypes when config-ref is part of model
    Optional<ComponentConfiguration> configuration = elementModel.getConfiguration();
    if (configuration.isPresent()) {
      String configRef = configuration.get().getParameters().get(CONFIG_ATTRIBUTE_NAME);
      if (!isBlank(configRef)) {
        return getHashedGlobal(configRef);
      }
    }

    return empty();
  }

  private Optional<MetadataCacheId> resolveGlobalElement(DslElementModel<?> elementModel) {
    List<MetadataCacheId> parts = new ArrayList<>();
    Predicate isRequiredForMetadata = parameterNamesRequiredForMetadataCacheId(elementModel.getModel())
        .map((names) -> (Predicate) ((model) -> isRequiredForMetadata(names, model))).orElse((model) -> true);
    elementModel.getContainedElements().stream()
        .filter(containedElement -> containedElement.getModel() != null)
        .filter(containedElement -> isRequiredForMetadata.test(containedElement.getModel()))
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
    if (model instanceof ParameterModel && parameterNamesRequiredForMetadataCacheId != null) {
      return parameterNamesRequiredForMetadataCacheId.contains(((ParameterModel) model).getName());
    } else {
      return true;
    }
  }

  private Optional<List<String>> parameterNamesRequiredForMetadataCacheId(Object model) {
    if (model instanceof EnrichableModel) {
      return ((EnrichableModel) model).getModelProperty(RequiredForMetadataModelProperty.class)
          .map(RequiredForMetadataModelProperty::getRequiredParameters);
    }
    return empty();
  }

  private Optional<MetadataCacheId> resolveMetadataKeyParts(DslElementModel<?> elementModel,
                                                            ComponentModel componentModel,
                                                            boolean resolveAllKeys) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    final TypeKeysResolver keyResolver = componentModel.getModelProperty(MetadataResolverFactoryModelProperty.class)
        .map(mp -> mp.getMetadataResolverFactory().getKeyResolver())
        .orElse(null);

    if (keyResolver != null) {
      keyParts.add(new MetadataCacheId(keyResolver.getCategoryName().hashCode(), "category: " + keyResolver.getCategoryName()));
      keyParts.add(new MetadataCacheId(keyResolver.getResolverName().hashCode(), "resolver: " + keyResolver.getResolverName()));
    }

    boolean isPartialFetching = keyResolver instanceof PartialTypeKeysResolver;

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
    if (element == null || !element.getValue().isPresent()) {
      return empty();
    }

    final String value = element.getValue().get();
    final String sourceElementName = getModelName(element.getModel())
        .map(modelName -> isBlank(element.getDsl().getPrefix()) ? modelName : element.getDsl().getPrefix() + ":" + modelName)
        .orElseGet(() -> element.getIdentifier().map(Object::toString).orElse(null));

    final MetadataCacheId valuePart = new MetadataCacheId(value.hashCode(), sourceElementName);
    if (value.contains(DEFAULT_EXPRESSION_PREFIX)) {
      return of(valuePart);
    }

    Reference<MetadataCacheId> reference = new Reference<>();
    if (element.getModel() instanceof ParameterModel) {
      ParameterModel model = (ParameterModel) element.getModel();
      model.getType()
          .accept(new MetadataTypeVisitor() {

            @Override
            public void visitString(StringType stringType) {
              if (!model.getAllowedStereotypes().isEmpty()) {
                getHashedGlobal(value).ifPresent(reference::set);
              }
            }

            @Override
            public void visitArrayType(ArrayType arrayType) {
              if (model.getDslConfiguration().allowsReferences()) {
                getHashedGlobal(value).ifPresent(reference::set);
              }
            }

            @Override
            public void visitObject(ObjectType objectType) {
              if (model.getDslConfiguration().allowsReferences()) {
                getHashedGlobal(value).ifPresent(reference::set);
              }
            }

          });

    } else if (element.getModel() instanceof MetadataType) {
      ((MetadataType) element.getModel()).accept(new MetadataTypeVisitor() {

        @Override
        public void visitArrayType(ArrayType arrayType) {
          getHashedGlobal(value).ifPresent(reference::set);
        }

        @Override
        public void visitObject(ObjectType objectType) {
          boolean canBeGlobal = objectType.getAnnotation(TypeDslAnnotation.class)
              .map(TypeDslAnnotation::allowsTopLevelDefinition).orElse(false);

          if (canBeGlobal) {
            getHashedGlobal(value).ifPresent(reference::set);
          }
        }

      });
    } else {
      LOGGER.warn(format("Unknown model type '%s' found for element '%s'", String.valueOf(element.getModel()),
                         element.getIdentifier().map(Object::toString).orElse(sourceElementName)));
    }

    return of(reference.get() == null ? valuePart : reference.get());
  }

  private Optional<MetadataCacheId> getHashedGlobal(String name) {
    if (!isBlank(name)) {
      return locator.get(Location.builder().globalName(name).build())
          .map(global -> getIdForComponentMetadata(global).orElse(null));
    }
    return empty();
  }

  private Optional<String> getModelName(Object model) {
    if (model instanceof NamedObject) {
      return of(((NamedObject) model).getName());
    }

    if (model instanceof ObjectType) {
      return ExtensionMetadataTypeUtils.getId((MetadataType) model);
    }

    return empty();
  }
}
