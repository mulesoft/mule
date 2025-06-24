/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.provider;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureMetadata;
import org.mule.runtime.feature.internal.togglz.MuleTogglzProfilingFeature;
import org.mule.runtime.feature.internal.togglz.MuleTogglzRuntimeFeature;
import org.togglz.core.Feature;
import org.togglz.core.metadata.FeatureMetaData;
import org.togglz.core.metadata.enums.EnumFeatureMetaData;
import org.togglz.core.spi.FeatureProvider;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 * Implementation of {@link FeatureProvider} for the Mule Runtime.
 *
 * @since 4.5.0
 */
public class DefaultMuleTogglzFeatureProvider implements MuleTogglzFeatureProvider {

  public static final String RUNTIME_FEATURE_NOT_REGISTERED = "Runtime feature %s not registered";
  public static final String FEATURE_ENUMS_NOT_NULL = "The featureEnums argument must not be null";
  public static final String ARGUMENT_MUST_BE_ENUM = "The featureEnum argument must be an enum";
  public static final String FEATURE_HAS_ALREADY_BEEN_ADDED = "The '%s' feature has already been added";
  public static final String CONSUMER_NAME_MUST_NOT_BE_NULL = "Consumer name must not be null.";
  public static final String FEATURE_NAME_MUST_NOT_BE_NULL = "Feature name must not be null.";

  private final Cache<String, MuleTogglzRuntimeFeature> runtimeFeaturesCache =
      Caffeine.newBuilder().build();

  private final Cache<ProfilingEventType<?>, Map<String, MuleTogglzProfilingFeature>> profilingEventTypesFeatures =
      Caffeine.newBuilder().build();

  private final Map<String, FeatureMetaData> metadataCache = new ConcurrentHashMap<>();
  private final Map<String, Feature> features = new ConcurrentHashMap<>();

  public DefaultMuleTogglzFeatureProvider(Class<? extends Feature> initialFeatureEnum) {
    if (initialFeatureEnum == null) {
      throw new IllegalArgumentException(FEATURE_ENUMS_NOT_NULL);
    }
    addFeatureEnum(initialFeatureEnum);
  }

  /**
   * Adds as {@link Feature}'s each of the enums items provided by the class.
   *
   * @param featureEnum the class containing the enum
   */
  public void addFeatureEnum(Class<? extends Feature> featureEnum) {
    if (featureEnum == null || !featureEnum.isEnum()) {
      throw new IllegalArgumentException(ARGUMENT_MUST_BE_ENUM);
    }
    addFeatures(asList(featureEnum.getEnumConstants()));
  }

  private void addFeatures(Collection<? extends Feature> newFeatures) {
    for (Feature newFeature : newFeatures) {
      if (metadataCache.put(newFeature.name(), new EnumFeatureMetaData(newFeature)) != null) {
        throw new IllegalArgumentException(format(FEATURE_HAS_ALREADY_BEEN_ADDED, newFeature.name()));
      }

      features.put(newFeature.name(), newFeature);
    }
  }

  @Override
  public Set<Feature> getFeatures() {
    return new HashSet<>(features.values());
  }

  @Override
  public FeatureMetaData getMetaData(Feature feature) {
    return metadataCache.get(feature.name());
  }

  @Override
  public Feature getRuntimeTogglzFeature(org.mule.runtime.api.config.Feature feature) {
    return runtimeFeaturesCache.get(feature.getName(), ft -> {
      throw new IllegalArgumentException(format(RUNTIME_FEATURE_NOT_REGISTERED, feature));
    });
  }

  @Override
  public MuleTogglzRuntimeFeature getOrRegisterRuntimeTogglzFeatureFrom(org.mule.runtime.api.config.Feature feature) {
    return runtimeFeaturesCache.get(feature.getName(), key -> newRuntimeTogglzFeature(feature));
  }

  private MuleTogglzRuntimeFeature newRuntimeTogglzFeature(org.mule.runtime.api.config.Feature ft) {
    if (ft == null) {
      throw new IllegalArgumentException(FEATURE_NAME_MUST_NOT_BE_NULL);
    }
    MuleTogglzRuntimeFeature newFeature = new MuleTogglzRuntimeFeature(ft);
    addTogglzFeatureMetadata(newFeature);
    return newFeature;
  }

  private void addTogglzFeatureMetadata(Feature newFeature) {
    if (metadataCache.put(newFeature.name(), new MuleTogglzFeatureMetadata(newFeature)) != null) {
      return;
    }

    features.put(newFeature.name(), newFeature);
  }

  @Override
  public MuleTogglzProfilingFeature getOrRegisterProfilingTogglzFeatureFrom(ProfilingEventType<?> profilingEventType,
                                                                            String consumerName) {
    if (consumerName == null) {
      throw new IllegalArgumentException(CONSUMER_NAME_MUST_NOT_BE_NULL);
    }
    MuleTogglzProfilingFeature consumerFeature = new MuleTogglzProfilingFeature(profilingEventType, consumerName);
    addTogglzFeatureMetadata(consumerFeature);
    return profilingEventTypesFeatures
        .get(profilingEventType, profEventType -> new ConcurrentHashMap<>())
        .computeIfAbsent(consumerName, name -> consumerFeature);
  }

  public Collection<MuleTogglzProfilingFeature> getConsumerFeaturesFor(ProfilingEventType<?> profilingEventType) {
    return profilingEventTypesFeatures
        .get(profilingEventType, profEventType -> new ConcurrentHashMap<>())
        .values();
  }

  @Override
  public Feature getFeature(String featureName) {
    if (featureName == null) {
      throw new IllegalArgumentException(FEATURE_NAME_MUST_NOT_BE_NULL);
    }
    return features.get(featureName);
  }
}
