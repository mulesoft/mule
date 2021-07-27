/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy.util;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.profiling.type.ProfilingEventType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.processor.chain.InterceptedReactiveProcessor;
import org.mule.runtime.core.privileged.processor.chain.HasLocation;

import static org.mule.runtime.core.internal.profiling.notification.ProfilingNotification.*;

/**
 * Utility methods for handling profiling of processing strategy.
 * <p>
 * MULE-19594: Handle expression problem for Reactive Processor (ComponentLocation)
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
   * @return the artifact id if possible.
   */
  public static String getArtifactId(MuleContext muleContext) {
    if (muleContext.getConfiguration() == null) {
      return "UNKNOWN_ARTIFACT_ID";
    }

    return muleContext.getConfiguration().getId();
  }

  /**
   * @param muleContext the {@link MuleContext} from which to return the artifactId
   * @return the artifact id if possible.
   */
  public static String getArtifactType(MuleContext muleContext) {
    if (muleContext.getArtifactType() == null) {
      return "UNKNOWN_ARTIFACT_TYPE";
    }

    return muleContext.getArtifactType().getAsString();
  }

  /**
   * @return the fully qualified profiling notification identifier considering the namespace.
   */
  public static String getFullyQualifiedProfilingNotificationIdentifier(ProfilingEventType profilingEventType) {
    return profilingEventType.getProfilingEventTypeNamespace() + PROFILING_NAMESPACE_IDENTIFIER_SEPARATOR
        + profilingEventType
            .getProfilingEventTypeIdentifier();
  }
}
