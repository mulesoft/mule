/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.metadata;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.metadata.resolving.FailureCode.CONNECTION_FAILURE;
import static org.mule.runtime.api.metadata.resolving.MetadataFailure.Builder.newFailure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.failure;
import static org.mule.runtime.api.metadata.resolving.MetadataResult.success;
import static org.mule.runtime.module.extension.api.metadata.MultilevelMetadataKeyBuilder.newKey;
import static org.mule.runtime.module.extension.internal.metadata.MetadataResolverUtils.resolveWithOAuthRefresh;
import static org.mule.runtime.module.extension.internal.util.IntrospectionUtils.getField;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.parameter.ParameterModel;
import org.mule.runtime.api.metadata.MetadataContext;
import org.mule.runtime.api.metadata.MetadataKey;
import org.mule.runtime.api.metadata.MetadataKeyBuilder;
import org.mule.runtime.api.metadata.MetadataKeyProvider;
import org.mule.runtime.api.metadata.MetadataKeysContainer;
import org.mule.runtime.api.metadata.MetadataKeysContainerBuilder;
import org.mule.runtime.api.metadata.resolving.MetadataResult;
import org.mule.runtime.api.metadata.resolving.PartialTypeKeysResolver;
import org.mule.runtime.api.metadata.resolving.TypeKeysResolver;
import org.mule.runtime.extension.api.annotation.metadata.MetadataKeyId;
import org.mule.runtime.extension.api.metadata.NullMetadataKey;
import org.mule.runtime.extension.api.property.MetadataKeyPartModelProperty;
import org.mule.runtime.module.extension.api.metadata.MultilevelMetadataKeyBuilder;
import org.mule.runtime.module.extension.internal.loader.java.property.DeclaringMemberModelProperty;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import com.google.common.collect.ImmutableSet;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Metadata service delegate implementations that handles the resolution of a {@link ComponentModel} {@link MetadataKeysContainer}
 *
 * @since 4.0
 */
class MetadataKeysDelegate extends BaseMetadataDelegate {

  private static final int INITIAL_PART_LEVEL = 1;
  private final List<ParameterModel> keyParts;

  MetadataKeysDelegate(ComponentModel componentModel, List<ParameterModel> metadataKeyParts) {
    super(componentModel);
    keyParts = metadataKeyParts;
  }

  /**
   * Resolves the list of types available for the Content or Output of the associated {@link MetadataKeyProvider} Component,
   * representing them as a list of {@link MetadataKey}.
   * <p>
   * If no {@link MetadataKeyId} is present in the component's input parameters, then a {@link NullMetadataKey} is returned.
   * Otherwise, the {@link TypeKeysResolver#getKeys} associated with the current Component will be invoked to obtain the keys
   *
   * @param context current {@link MetadataContext} that will be used by the {@link TypeKeysResolver}
   * @return Successful {@link MetadataResult} if the keys are obtained without errors Failure {@link MetadataResult} when no
   *         Dynamic keys are a available or the retrieval fails for any reason
   */
  MetadataResult<MetadataKeysContainer> getMetadataKeys(MetadataContext context, ReflectionCache reflectionCache) {
    return getMetadataKeys(context, null, reflectionCache);
  }

  MetadataResult<MetadataKeysContainer> getMetadataKeys(MetadataContext context, Object partialKey,
                                                        ReflectionCache reflectionCache) {
    final TypeKeysResolver keyResolver = resolverFactory.getKeyResolver();
    final String componentResolverName = keyResolver.getCategoryName();
    final MetadataKeysContainerBuilder keysContainer = MetadataKeysContainerBuilder.getInstance();

    if (keyParts.isEmpty()) {
      return success(keysContainer.add(componentResolverName, ImmutableSet.of(new NullMetadataKey())).build());
    }

    try {
      final Map<Integer, ParameterModel> partsByOrder = getPartOrderMapping(keyParts);
      Set<MetadataKey> metadataKeys =
          resolveWithOAuthRefresh(context,
                                  () -> getMetadataKeys(context, keyResolver, partialKey, reflectionCache, partsByOrder));

      final Set<MetadataKey> enrichedMetadataKeys = metadataKeys.stream()
          .map(metadataKey -> cloneAndEnrichMetadataKey(metadataKey, partsByOrder))
          .map(MetadataKeyBuilder::build).collect(toCollection(LinkedHashSet::new));
      keysContainer.add(componentResolverName, enrichedMetadataKeys);

      return success(keysContainer.build());
    } catch (ConnectionException e) {
      return failure(newFailure(e).withMessage("Failed to establish connection: " + ExceptionUtils.getMessage(e))
          .withFailureCode(CONNECTION_FAILURE).onKeys());
    } catch (Exception e) {
      return failure(newFailure(e).onKeys());
    }
  }

  private Set<MetadataKey> getMetadataKeys(MetadataContext context, TypeKeysResolver keyResolver, Object partialKey,
                                           ReflectionCache reflectionCache, Map<Integer, ParameterModel> partsByOrder)
      throws Exception {
    if (keyResolver instanceof PartialTypeKeysResolver && hasInitialLevel(partialKey, partsByOrder, reflectionCache)) {
      return singleton(((PartialTypeKeysResolver) keyResolver).resolveChilds(context, partialKey));
    } else {
      return keyResolver.getKeys(context);
    }
  }

  private boolean hasInitialLevel(Object keyValue, Map<Integer, ParameterModel> partsByOrder, ReflectionCache reflectionCache) {
    if (keyValue == null) {
      return false;
    }

    Optional<DeclaringMemberModelProperty> member =
        partsByOrder.get(INITIAL_PART_LEVEL).getModelProperty(DeclaringMemberModelProperty.class);

    if (!member.isPresent()) {
      return false;
    }

    return getField(keyValue.getClass(), member.get().getDeclaringField().getName(), reflectionCache)
        .map(field -> {
          field.setAccessible(true);
          try {
            return field.get(keyValue) != null;
          } catch (Exception e) {
            return false;
          }
        })
        .orElse(false);
  }

  /**
   * Introspect the {@link List} of {@link ParameterModel} of the {@link ComponentModel} and filter the ones that are parts of the
   * {@link MetadataKey} and creates a mapping with the order number of each part with their correspondent name.
   *
   * @param parameterModels of the {@link ComponentModel}
   * @return the mapping of the order number of each part with their correspondent name
   */
  private Map<Integer, ParameterModel> getPartOrderMapping(List<ParameterModel> parameterModels) {
    return parameterModels.stream()
        .filter(this::isKeyPart)
        .collect(toMap(part -> part.getModelProperty(MetadataKeyPartModelProperty.class).get().getOrder(), part -> part));
  }

  private boolean isKeyPart(ParameterModel part) {
    return part.getModelProperty(MetadataKeyPartModelProperty.class).isPresent();
  }

  private MetadataKeyBuilder cloneAndEnrichMetadataKey(MetadataKey key, Map<Integer, ParameterModel> partOrderMapping) {
    return cloneAndEnrichMetadataKey(key, partOrderMapping, INITIAL_PART_LEVEL);
  }

  /**
   * Given a {@link MetadataKey}, this is navigated recursively cloning each {@link MetadataKey} of the tree structure creating a
   * {@link MultilevelMetadataKeyBuilder} and adding the partName of each {@link MetadataKey} found.
   *
   * @param key              {@link MetadataKey} to be cloned and enriched
   * @param partOrderMapping {@link Map} that contains the mapping of the name of each part of the {@link MetadataKey}
   * @param level            the current level of the part of the {@link MetadataKey} to be cloned and enriched
   * @return a {@link MetadataKeyBuilder} with the cloned and enriched keys
   */
  private MetadataKeyBuilder cloneAndEnrichMetadataKey(MetadataKey key, Map<Integer, ParameterModel> partOrderMapping,
                                                       int level) {
    final MetadataKeyBuilder keyBuilder =
        newKey(key.getId(), partOrderMapping.get(level).getName()).withDisplayName(key.getDisplayName());
    key.getProperties().forEach(keyBuilder::withProperty);
    key.getChilds().forEach(childKey -> keyBuilder.withChild(cloneAndEnrichMetadataKey(childKey, partOrderMapping, level + 1)));
    return keyBuilder;
  }
}
