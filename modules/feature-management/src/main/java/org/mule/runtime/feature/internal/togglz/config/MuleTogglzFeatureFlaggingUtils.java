/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.feature.internal.togglz.config;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.feature.internal.togglz.MuleTogglzFeatureManagerProvider.FEATURE_PROVIDER;
import static org.togglz.core.context.FeatureContext.getFeatureManager;
import static org.togglz.core.user.thread.ThreadLocalUserProvider.bind;
import static org.togglz.core.user.thread.ThreadLocalUserProvider.release;

import org.mule.runtime.api.config.Feature;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.feature.internal.togglz.user.MuleTogglzArtifactFeatureUser;
import org.togglz.core.repository.FeatureState;
import org.togglz.core.user.FeatureUser;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Utility methods for feature flagging, bridging mule API with togglz.
 *
 * @since 4.5.0
 */
public class MuleTogglzFeatureFlaggingUtils {

  private MuleTogglzFeatureFlaggingUtils() {}

  /**
   * Adds a runtime feature as togglz feature. This will be registered as a {@link Feature} and will be available for verifying if
   * it is enabled/disabled, activated/deactivated through Togglz.
   *
   * @param feature the {@link Feature} to be added.
   */
  public static void addMuleTogglzRuntimeFeature(Feature feature) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());
      FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(feature);
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  /**
   * Registers the runtime features as togglz features. Used for bridging the Feature Flagging Service API with Togglz.
   *
   * @param artifactId the artifact id that will be associated to the status.
   * @param features   a map indicating what features are enabled/disabled for an artifactId.
   * @return a map with the {@link FeatureState} for each feature.
   */
  public static MuleTogglzManagedArtifactFeatures getTogglzManagedArtifactFeatures(String artifactId,
                                                                                   Map<Feature, Boolean> features) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());
      return withFeatureUser(new MuleTogglzArtifactFeatureUser(artifactId), () -> {
        Map<org.togglz.core.Feature, FeatureState> togglzFeatureStates = new HashMap<>();
        features.forEach((feature, featureStatus) -> {
          org.togglz.core.Feature togglzFeature = FEATURE_PROVIDER.getOrRegisterRuntimeTogglzFeatureFrom(feature);
          getFeatureManager().setFeatureState(new FeatureState(togglzFeature, featureStatus));
          togglzFeatureStates.put(togglzFeature, getFeatureManager().getFeatureState(togglzFeature));
        });
        return new MuleTogglzManagedArtifactFeatures(artifactId, togglzFeatureStates);
      });
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  /**
   * Sets the {@link FeatureState} corresponding to a {@link org.togglz.core.Feature}
   *
   * @param featureState the {@link FeatureState}
   */
  public static void setFeatureState(FeatureState featureState) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());
      getFeatureManager().setFeatureState(featureState);
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  /**
   * Executes a runnable within the context of the {@link FeatureUser}. Every operation involved regarding a
   * {@link org.togglz.core.Feature} take into account the user.
   *
   * @param featureUser the {@link FeatureUser} to be bound to the context.
   * @param runnable    the {@link Runnable} to be executed.
   */
  public static void withFeatureUser(FeatureUser featureUser, Runnable runnable) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());
      FeatureUser previousFeatureUser = null;
      try {
        previousFeatureUser = getFeatureManager().getCurrentFeatureUser();
        release();
        bind(featureUser);
        runnable.run();
      } finally {
        release();
        bind(previousFeatureUser);
      }
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  /**
   * Executes a {@link Runnable} within the context of a {@link FeatureUser}. Every operation involved regarding a
   * {@link org.togglz.core.Feature} will take into account this user.
   *
   * @param featureUser the {@link FeatureUser} to be bound to the context.
   * @param callable    the {@link Callable} to be called.
   * @return the return value from the {@link Callable}
   */
  public static <T> T withFeatureUser(FeatureUser featureUser, Callable<T> callable) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());

      FeatureUser previousFeatureUser = null;
      try {
        previousFeatureUser = getFeatureManager().getCurrentFeatureUser();
        release();
        bind(featureUser);
        return callable.call();
      } catch (Exception e) {
        throw new MuleRuntimeException(e);
      } finally {
        release();
        bind(previousFeatureUser);
      }
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  /**
   * Gets a {@link org.togglz.core.Feature} by name.
   *
   * @param featureName the feature name
   * @return {@link org.togglz.core.Feature} corresponding to that name.
   */
  public static org.togglz.core.Feature getFeature(String featureName) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());
      return FEATURE_PROVIDER.getFeature(featureName);
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  public static FeatureState getFeatureState(org.togglz.core.Feature feature) {
    ClassLoader cl = currentThread().getContextClassLoader();
    try {
      currentThread().setContextClassLoader(MuleTogglzFeatureFlaggingUtils.class.getClassLoader());
      return getFeatureManager().getFeatureState(feature);
    } finally {
      currentThread().setContextClassLoader(cl);
    }
  }

  /**
   * @param profilingEventType the {@link ProfilingEventType}.
   * @param prefix             the consumer name
   * @param <T>                the class of {@link ProfilingEventType}
   * @return a fully qualified name for a feature used as id.
   */
  public static <T extends ProfilingEventContext> String getFullyQualifiedProfilingEventTypeFeatureIdentifier(
                                                                                                              ProfilingEventType<T> profilingEventType,
                                                                                                              String prefix) {
    return prefix + ":" + getFullyQualifiedProfilingEventTypeIdentifier(profilingEventType);
  }

  /**
   * @param profilingEventType the {@link ProfilingEventType}
   * @param <T>                the {@link ProfilingEventContext} associated to the profilingEventType.
   * @return the fqn.
   */
  public static <T extends ProfilingEventContext> String getFullyQualifiedProfilingEventTypeIdentifier(
                                                                                                       ProfilingEventType<T> profilingEventType) {
    return profilingEventType.getProfilingEventTypeNamespace() + ":" + profilingEventType.getProfilingEventTypeIdentifier();
  }
}
