/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.core.internal.message.EventInternalContext;
import org.mule.runtime.core.internal.message.InternalEvent;
import org.mule.runtime.core.internal.policy.SourcePolicyContext;

public final class RoutingUtils {

  private RoutingUtils() {

  }

  public static void setSourcePolicyChildContext(InternalEvent event) {
    EventInternalContext<?> context = event.getSourcePolicyContext();
    SourcePolicyContext policyContext = (SourcePolicyContext) context;
    event.setSourcePolicyContext(policyContext.childContext());
  }

}
