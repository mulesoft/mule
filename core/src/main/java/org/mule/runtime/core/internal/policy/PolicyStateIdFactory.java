/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.policy.PolicyStateId;
import org.mule.runtime.core.privileged.event.BaseEventContext;

public class PolicyStateIdFactory {

  private final String policyId;

  public PolicyStateIdFactory(String policyId) {
    this.policyId = policyId;
  }

  /**
   * Creates a {@link PolicyStateId} for a given event
   * 
   * @param coreEvent the given event
   * @return the identifier of the policy state for the given event
   */
  public PolicyStateId create(CoreEvent coreEvent) {
    return new PolicyStateId(rootEventId(coreEvent), policyId);
  }

  private String rootEventId(CoreEvent event) {
    return ((BaseEventContext) event.getContext()).getRootContext().getId();
  }
}
