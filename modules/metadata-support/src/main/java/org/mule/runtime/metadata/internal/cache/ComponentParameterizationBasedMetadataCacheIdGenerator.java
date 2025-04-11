/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.metadata.internal.cache;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;
import static org.mule.runtime.api.util.Preconditions.checkArgument;

import static java.lang.String.format;
import static java.util.Comparator.comparingInt;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.meta.model.config.ConfigurationModel;
import org.mule.runtime.api.meta.model.parameter.ParameterGroupModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.meta.model.stereotype.StereotypeModel;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.internal.util.cache.CacheIdBuilderAdapter;
import org.mule.runtime.extension.api.property.MetadataKeyIdModelProperty;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.extension.api.property.TypeResolversInformationModelProperty;
import org.mule.runtime.metadata.api.cache.ComponentParameterizationMetadataCacheIdGenerator;
import org.mule.runtime.metadata.api.cache.ConfigurationMetadataCacheIdGenerator;
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

  private static final String CONFIG_ATTRIBUTE_NAME = "config-ref";
  private static final String NAME_ATTRIBUTE_NAME = "name";

  private final ConfigurationMetadataCacheIdGenerator configIdGenerator;

  public ComponentParameterizationBasedMetadataCacheIdGenerator(ConfigurationMetadataCacheIdGenerator configIdGenerator) {
    this.configIdGenerator = configIdGenerator;
  }

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
      resolveComponentIdentifierMetadataCacheId(parameterization).ifPresent(keyParts::add);
      if (parameterization.getModel() instanceof ComponentModel) {
        keyParts.add(resolveStereotypeId((ComponentModel) parameterization.getModel()));
      }
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

  private Optional<MetadataCacheId> doResolve(ComponentParameterization<?> parameterization) {
    List<MetadataCacheId> keyParts = new ArrayList<>();

    resolveConfigId(parameterization).ifPresent(keyParts::add);

    resolveCategoryId(parameterization).ifPresent(keyParts::add);

    resolveComponentIdentifierMetadataCacheId(parameterization).ifPresent(keyParts::add);

    if (parameterization.getModel() instanceof ComponentModel) {
      resolveMetadataKeyParts(parameterization, (ComponentModel) parameterization.getModel(), true).ifPresent(keyParts::add);
    } else {
      resolveNamedConfigId(parameterization).ifPresent(keyParts::add);
    }

    if (keyParts.isEmpty()) {
      return empty();
    } else {
      return of(new MetadataCacheId(keyParts, sourceElementName(parameterization)));
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
      resolveComponentExtensionMetadataCacheId(parameterization).ifPresent(keyParts::add);
      resolveConfigId(parameterization).ifPresent(keyParts::add);

      typeInformation.getResolverCategory()
          .ifPresent(resolverCategory -> keyParts.add(createCategoryMetadataCacheId(resolverCategory)));

      typeInformation.getResolverName().ifPresent(resolverName -> keyParts.add(createResolverMetadataCacheId(resolverName)));

      keyParts.add(typeInformation.getComponentTypeMetadataCacheId());

      if (parameterization.getModel() instanceof ComponentModel) {
        resolveMetadataKeyParts(parameterization, (ComponentModel) parameterization.getModel(),
                                typeInformation.shouldIncludeConfiguredMetadataKeys())
            .ifPresent(keyParts::add);
      }
    } else {
      resolveComponentIdentifierMetadataCacheId(parameterization).ifPresent(keyParts::add);

      if (parameterization.getModel() instanceof ConfigurationModel) {
        resolveNamedConfigId(parameterization).ifPresent(keyParts::add);
      }

      keyParts.add(typeInformation.getComponentTypeMetadataCacheId());
    }

    return of(new MetadataCacheId(keyParts,
                                  typeInformation.getComponentTypeMetadataCacheId().getSourceElementName()
                                      .map(sourceElementName -> format("(%s):(%s)", sourceElementName(parameterization),
                                                                       sourceElementName))
                                      .orElse(format("(%s):(%s)", sourceElementName(parameterization), "Unknown Type"))));
  }

  private Optional<MetadataCacheId> resolveComponentExtensionMetadataCacheId(ComponentParameterization<?> parameterization) {
    return parameterization.getComponentIdentifier().map(ComponentIdentifier::getNamespace)
        .map(namespace -> new MetadataCacheId(namespace.toLowerCase().hashCode(), namespace));
  }

  private Optional<MetadataCacheId> resolveComponentIdentifierMetadataCacheId(ComponentParameterization<?> parameterization) {
    return parameterization.getComponentIdentifier().map(this::resolveForIdentifier);
  }

  private MetadataCacheId resolveForIdentifier(ComponentIdentifier id) {
    return new MetadataCacheId(id.hashCode(), id.toString());
  }

  private MetadataCacheId resolveStereotypeId(ComponentModel model) {
    StereotypeModel stereotype = model.getStereotype();
    return new MetadataCacheId(stereotype.hashCode(), stereotype.toString());
  }

  private Optional<MetadataCacheId> resolveConfigId(ComponentParameterization<?> parameterization) {
    return resolveConfigName(parameterization).flatMap(name -> this.configIdGenerator.getConfigMetadataCacheId(name, false));
  }

  private Optional<MetadataCacheId> resolveNamedConfigId(ComponentParameterization<?> parameterization) {
    return resolveName(parameterization).flatMap(name -> this.configIdGenerator.getConfigMetadataCacheId(name, true));
  }

  private MetadataCacheId createCategoryMetadataCacheId(String category) {
    return new MetadataCacheId(category.hashCode(), "category: " + category);
  }

  private MetadataCacheId createResolverMetadataCacheId(String resolverName) {
    return new MetadataCacheId(resolverName.hashCode(), "resolver: " + resolverName);
  }

  public static String sourceElementName(ComponentParameterization<?> parameterization) {
    return parameterization.getComponentIdentifier().map(id -> id.getNamespace() + ":").orElse("")
        + parameterization.getModel().getName();
  }

  public static Optional<String> resolveConfigName(ComponentParameterization<?> parameterization) {
    return ofNullable(parameterization.getParameter(DEFAULT_GROUP_NAME, CONFIG_ATTRIBUTE_NAME)).map(param -> (String) param);
  }

  public static Optional<String> resolveName(ComponentParameterization<?> parameterization) {
    return ofNullable(parameterization.getParameter(DEFAULT_GROUP_NAME, NAME_ATTRIBUTE_NAME)).map(param -> (String) param);
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
    return computeIdFor(parameterization, groupModel, param, MetadataCacheIdBuilderAdapter::new);
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

}
