/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.internal.dsl.DslConstants.CONFIG_ATTRIBUTE_NAME;

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import static org.apache.commons.lang.StringUtils.isBlank;

import org.mule.metadata.api.model.ArrayType;
import org.mule.metadata.api.model.ObjectType;
import org.mule.metadata.api.model.StringType;
import org.mule.metadata.api.visitor.MetadataTypeVisitor;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.EnrichableModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.internal.util.cache.CacheIdBuilderAdapter;
import org.mule.runtime.extension.api.component.ComponentParameterization;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.RequiredForMetadataModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.metadata.api.cache.ComponentParameterizationMetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.MetadataCacheId;
import org.mule.runtime.metadata.internal.types.AttributesMetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.ComponentParameterizationInputMetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.KeysMetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.MetadataResolutionTypeInformation;
import org.mule.runtime.metadata.internal.types.OutputMetadataResolutionTypeInformation;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * MetadataCacheIdGenerator for Diet implementation of TypeResolution, to generate keys from {@link ComponentParameterization}.
 * This is based on {@link ComponentAstBasedMetadataCacheIdGenerator}, from Mule Spring Config, used in Mule Runtime Tooling
 * Client, to generate the keys from the ComponentAst.
 */
public class ComponentParameterizationBasedMetadataCacheIdGenerator implements ComponentParameterizationMetadataCacheIdGenerator {

  @Override
  public Optional<MetadataCacheId> getIdForComponentOutputMetadata(ComponentParameterization<?> parameterization) {
    if (parameterization.getModel() instanceof HasOutputModel) {
      return doResolveType(parameterization, new OutputMetadataResolutionTypeInformation(parameterization));
    } else {
      return empty();
    }
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentAttributesMetadata(ComponentParameterization<?> parameterization) {
    if (parameterization.getModel() instanceof HasOutputModel) {
      return doResolveType(parameterization, new AttributesMetadataResolutionTypeInformation(parameterization));
    } else {
      return empty();
    }
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentParameterization<?> parameterization,
                                                                  String parameterName) {
    checkArgument(parameterization.getModel().getAllParameterModels().stream()
        .anyMatch(parameterModel -> parameterModel.getName().equals(parameterName)),
                  () -> "Cannot generate an Input Cache Key for component '" + parameterization.getModel().getName()
                      + "' since it does not have a parameter named "
                      + parameterName);
    return doResolveType(parameterization,
                         new ComponentParameterizationInputMetadataResolutionTypeInformation(parameterization, parameterName));
  }


  @Override
  public Optional<MetadataCacheId> getIdForComponentInputMetadata(ComponentParameterization<?> parameterization,
                                                                  String parameterGroupName, String parameterName) {
    checkArgument(parameterization.getParameter(parameterGroupName, parameterName) != null,
                  () -> "Cannot generate an Input Cache Key for component '" + parameterization.getModel().getName()
                      + "' since it does not have a parameter with parameter group name " + parameterGroupName + ", named "
                      + parameterName);
    return doResolveType(parameterization,
                         new ComponentParameterizationInputMetadataResolutionTypeInformation(parameterization, parameterGroupName,
                                                                                             parameterName));
  }

  @Override
  public Optional<MetadataCacheId> getIdForComponentMetadata(ComponentParameterization<?> parameterization) {
    return doResolve(parameterization);
  }

  @Override
  public Optional<MetadataCacheId> getIdForMetadataKeys(ComponentParameterization<?> parameterization) {
    return doResolveType(parameterization, new KeysMetadataResolutionTypeInformation(parameterization));
  }

  @Override
  public Optional<MetadataCacheId> getIdForGlobalMetadata(ComponentParameterization<?> parameterization) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    if (parameterization.getModel() instanceof ConfigurationModel) {
      if (parameterization.getModel() instanceof ComponentModel) {
        keyParts.add(resolveStereotypeId((ComponentModel) parameterization.getModel()));
      }
      resolveGlobalElement(parameterization).ifPresent(keyParts::add);
      return of(new MetadataCacheId(keyParts, sourceElementName(parameterization)));
    } else {
      Optional<MetadataCacheId> configId = resolveConfigId(parameterization);
      if (configId.isPresent()) {
        keyParts.add(configId.get());
        resolveCategoryId(parameterization).ifPresent(keyParts::add);
        return of(new MetadataCacheId(keyParts, sourceElementName(parameterization)));
      } else {
        if (parameterization.getModel() instanceof ComponentModel) {
          return of(resolveStereotypeId((ComponentModel) parameterization.getModel()));
        } else {
          return empty();
        }
      }
    }
  }

  private Optional<MetadataCacheId> resolveCategoryId(ComponentParameterization<?> parameterization) {
    if (!(parameterization.getModel() instanceof ComponentModel)) {
      return empty();
    }
    ComponentModel model = (ComponentModel) parameterization.getModel();
    return model.getModelProperty(MetadataKeyIdModelProperty.class).flatMap(MetadataKeyIdModelProperty::getCategoryName)
        .map(this::createCategoryMetadataCacheId);
  }

  private Optional<MetadataCacheId> doResolveType(ComponentParameterization<?> parameterization,
                                                  MetadataResolutionTypeInformation typeInformation) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    if (typeInformation.isDynamicType()) {
      resolveConfigId(parameterization).ifPresent(keyParts::add);

      typeInformation.getResolverCategory()
          .ifPresent(resolverCategory -> keyParts
              .add(createCategoryMetadataCacheId(resolverCategory)));

      typeInformation.getResolverName()
          .ifPresent(resolverName -> keyParts.add(createResolverMetadataCacheId(resolverName)));

      keyParts.add(typeInformation.getComponentTypeMetadataCacheId());

      if (parameterization.getModel() instanceof ComponentModel) {
        keyParts.add(resolveStereotypeId((ComponentModel) parameterization.getModel()));
        resolveMetadataKeyParts(parameterization, (ComponentModel) parameterization.getModel(),
                                typeInformation.shouldIncludeConfiguredMetadataKeys()).ifPresent(keyParts::add);
      }
    } else {
      if (parameterization.getModel() instanceof ComponentModel) {
        keyParts.add(resolveStereotypeId((ComponentModel) parameterization.getModel()));
      }

      if (parameterization.getModel() instanceof ConfigurationModel) {
        resolveGlobalElement(parameterization).ifPresent(keyParts::add);
      }
      keyParts.add(typeInformation.getComponentTypeMetadataCacheId());
    }

    return of(new MetadataCacheId(keyParts,
                                  typeInformation.getComponentTypeMetadataCacheId().getSourceElementName()
                                      .map(sourceElementName -> format("(%s):(%s)", sourceElementName(parameterization),
                                                                       sourceElementName))
                                      .orElse(format("(%s):(%s)", sourceElementName(parameterization), "Unknown Type"))));
  }

  private Optional<MetadataCacheId> doResolve(ComponentParameterization<?> parameterization) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    resolveConfigId(parameterization).ifPresent(keyParts::add);

    resolveCategoryId(parameterization).ifPresent(keyParts::add);

    if (parameterization.getModel() instanceof ComponentModel) {
      ComponentModel model = (ComponentModel) parameterization.getModel();
      keyParts.add(resolveStereotypeId(model));
      resolveMetadataKeyParts(parameterization, model, true).ifPresent(keyParts::add);
    } else {
      resolveGlobalElement(parameterization).ifPresent(keyParts::add);
    }

    return of(new MetadataCacheId(keyParts, sourceElementName(parameterization)));
  }

  private MetadataCacheId resolveStereotypeId(ComponentModel model) {
    StereotypeModel stereotype = model.getStereotype();
    return new MetadataCacheId(stereotype.hashCode(), stereotype.toString());
  }

  private Optional<MetadataCacheId> resolveConfigId(ComponentParameterization<?> parameterization) {
    return resolveConfigName(parameterization).flatMap(this::getHashedGlobal);
  }

  private Optional<MetadataCacheId> resolveGlobalElement(ComponentParameterization<?> parameterization) {
    List<String> parameterNamesRequiredForMetadata = parameterNamesRequiredForMetadataCacheId(parameterization);

    Map<Pair<ParameterGroupModel, ParameterModel>, Object> parameters = parameterization.getParameters();

    List<MetadataCacheId> parts = parameters.keySet()
        .stream()
        .filter(p -> parameterNamesRequiredForMetadata.contains(p.getSecond().getName()))
        .map(p -> resolveKeyFromSimpleValue(parameterization, p.getFirst(), p.getSecond()))
        .collect(toList());

    if (parts.isEmpty()) {
      return empty();
    }

    return of(new MetadataCacheId(parts, parameterization.getModel().getName()));
  }

  private List<String> parameterNamesRequiredForMetadataCacheId(ComponentParameterization<?> parameterization) {
    if (!(parameterization.getModel() instanceof EnrichableModel)) {
      return emptyList();
    }
    return ((EnrichableModel) parameterization.getModel()).getModelProperty(RequiredForMetadataModelProperty.class)
        .map(RequiredForMetadataModelProperty::getRequiredParameters).orElse(emptyList());
  }

  private Optional<MetadataCacheId> resolveMetadataKeyParts(ComponentParameterization<?> parameterization,
                                                            ComponentModel componentModel, boolean resolveAllKeys) {
    boolean isPartialFetching = componentModel.getModelProperty(TypeResolversInformationModelProperty.class)
        .map(TypeResolversInformationModelProperty::isPartialTypeKeyResolver)
        .orElse(false);

    if (!isPartialFetching && !resolveAllKeys) {
      return empty();
    }

    Map<Pair<ParameterGroupModel, ParameterModel>, Object> parameters = parameterization.getParameters();

    List<MetadataCacheId> keyParts = parameters.keySet()
        .stream()
        .filter(pair -> pair.getSecond().getModelProperty(MetadataKeyPartModelProperty.class).isPresent())
        .sorted(comparingInt(pair -> pair.getSecond().getModelProperty(MetadataKeyPartModelProperty.class).get().getOrder()))
        .map(p -> resolveKeyFromSimpleValue(parameterization, p.getFirst(), p.getSecond()))
        .collect(toList());
    return keyParts.isEmpty() ? empty() : of(new MetadataCacheId(keyParts, "metadataKeyValues"));
  }

  private MetadataCacheId resolveKeyFromSimpleValue(ComponentParameterization<?> parameterization, ParameterGroupModel groupModel,
                                                    ParameterModel param) {
    final MetadataCacheId notCheckingReferences =
        computeIdFor(parameterization, groupModel, param, MetadataCacheIdBuilderAdapter::new);
    if (parameterization.getParameter(groupModel, param) == null) {
      return notCheckingReferences;
    }

    parameterization.getParameter(groupModel, param);

    Reference<MetadataCacheId> reference = new Reference<>();

    param.getType().accept(new MetadataTypeVisitor() {

      @Override
      public void visitString(StringType stringType) {
        if (!param.getAllowedStereotypes().isEmpty()) {
          getHashedGlobal(param.toString()).ifPresent(reference::set);
        }
      }

      @Override
      public void visitArrayType(ArrayType arrayType) {
        if (param.getDslConfiguration().allowsReferences()) {
          getHashedGlobal(param.toString()).ifPresent(reference::set);
        }
      }

      @Override
      public void visitObject(ObjectType objectType) {
        if (param.getDslConfiguration().allowsReferences()) {
          getHashedGlobal(param.toString()).ifPresent(reference::set);
        }
      }

    });

    return reference.get() == null ? notCheckingReferences : reference.get();
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
    return of(new MetadataCacheId(name.hashCode(), "global: " + name));
  }

  public static String sourceElementName(ComponentParameterization<?> parameterization) {
    Map<Pair<ParameterGroupModel, ParameterModel>, Object> parameters = parameterization.getParameters();
    return parameters.keySet().stream().filter(p -> p.getSecond().getName().equals("config"))
        .map(p -> parameterization.getParameter(p.getFirst(), p.getSecond()).toString()).findFirst().orElse(null);
  }

  public static Optional<String> resolveConfigName(ComponentParameterization<?> parameterization) {
    return ofNullable(parameterization.getParameter(DEFAULT_GROUP_NAME, CONFIG_ATTRIBUTE_NAME)).map(param -> (String) param);
  }

  public static <K> K computeIdFor(ComponentParameterization<?> parameterization,
                                   ParameterGroupModel groupModel,
                                   ParameterModel parameter,
                                   Supplier<CacheIdBuilderAdapter<K>> cacheIdBuilderSupplier) {
    String name = parameter.getName();
    CacheIdBuilderAdapter<K> idBuilder =
        cacheIdBuilderSupplier.get().withSourceElementName(name).withHashValue(Objects.hashCode(name));
    idBuilder.withHashValue(Objects.hashCode(parameterization.getParameter(groupModel, parameter)));
    return idBuilder.build();
  }

  private static class MetadataCacheIdBuilderAdapter implements CacheIdBuilderAdapter<MetadataCacheId> {

    private String name;
    private int value;
    private final List<MetadataCacheId> parts = new ArrayList<>();

    @Override
    public CacheIdBuilderAdapter<MetadataCacheId> withSourceElementName(String name) {
      this.name = name;
      return this;
    }

    @Override
    public CacheIdBuilderAdapter<MetadataCacheId> withHashValue(int value) {
      this.value = value;
      return this;
    }

    @Override
    public CacheIdBuilderAdapter<MetadataCacheId> containing(List<MetadataCacheId> parts) {
      this.parts.addAll(parts);
      return this;
    }

    @Override
    public MetadataCacheId build() {
      if (parts.isEmpty()) {
        return new MetadataCacheId(value, name);
      }
      return new MetadataCacheId(parts, name);
    }
  }

}
