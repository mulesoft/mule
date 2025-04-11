/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.internal.event.InternalEvent;
import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.policy.SourcePolicyContext;

import static org.mule.runtime.api.config.MuleRuntimeFeature.CREATE_CHILD_POLICY_CONTEXT_FOR_PARALLEL_SCOPES;

/**
 * Util class for routers
 */
public final class RoutingUtils {

  private RoutingUtils() {

  }

  /**
   * Changes the {@link SourcePolicyContext} of the given event with a new instance (copy).
   *
   * @param event                  the event to change the {@link SourcePolicyContext}
   * @param featureFlaggingService The service to check if this change should be performed at all.
   */
  public static void setSourcePolicyChildContext(InternalEvent event, FeatureFlaggingService featureFlaggingService) {
    EventInternalContext<?> context = event.getSourcePolicyContext();
    if (!(context instanceof SourcePolicyContext)
        || !featureFlaggingService.isEnabled(CREATE_CHILD_POLICY_CONTEXT_FOR_PARALLEL_SCOPES)) {
      return;
    }
    SourcePolicyContext policyContext = (SourcePolicyContext) context;
    event.setSourcePolicyContext(policyContext.childContext());
  }

}
