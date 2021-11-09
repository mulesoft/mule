/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.util;

import static org.mule.runtime.core.internal.config.FeatureFlaggingUtils.withFeatureUser;
import static org.mule.runtime.core.internal.config.management.MuleTogglzProfilingFeatures.PROFILING_SERVICE_FEATURE;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.ProfilingEventContext;
import org.mule.runtime.api.profiling.ProfilingProducerScope;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.api.processor.HasLocation;
import org.togglz.core.user.FeatureUser;

/**
 * Utility methods for handling profiling of processing strategy.
 * <p>
 * MULE-19594: refactor the way of retrieving the component location from generic reactive processor.
 *
 * @since 4.0
 */
public final class ProfilingUtils {

  /**
   * @param processor the reactive processor from which the {@link ComponentLocation} has to be extracted.
   * @return the {@link ComponentLocation} if existing.
   */
  public static ComponentLocation getLocation(ReactiveProcessor processor) {
    if (processor instanceof HasLocation) {
      return ((HasLocation) processor).resolveLocation();
    }

    if (processor instanceof InterceptedReactiveProcessor) {
      return getLocation(((InterceptedReactiveProcessor) processor).getProcessor());
    }

    if (processor instanceof Component) {
      return ((Component) processor).getLocation();
    }

    return null;
  }

  /**
   * @param muleContext the {@link MuleContext} from which to return the artifactId
   * @return the artifact id.
   */
  public static String getArtifactId(MuleContext muleContext) {
    return muleContext.getConfiguration().getId();
  }

  /**
   * @param muleContext the {@link MuleContext} from which to return the artifactId
   * @return the artifact type.
   */
  public static String getArtifactType(MuleContext muleContext) {
    return muleContext.getArtifactType().getAsString();
  }

  public static boolean isProfilingServiceActive(FeatureUser featureUser) {
    return withFeatureUser(featureUser, () -> PROFILING_SERVICE_FEATURE.isActive());
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

  /**
   * @param profilingProducerContext the {@link org.mule.runtime.api.profiling.ProfilingDataProducer}
   * @return the fqn.
   */
  public static String getFullyQualifiedProlingProducerContextIdentifier(ProfilingProducerScope profilingProducerContext) {
    return profilingProducerContext.getProducerScopeTypeIdentifier() + ":" + profilingProducerContext
        .getProducerScopeIdentifier();
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
}
