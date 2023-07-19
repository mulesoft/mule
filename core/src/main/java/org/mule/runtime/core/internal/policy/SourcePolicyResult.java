/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;

/**
 * The result of processing a {@link CoreEvent} through a policy chain.
 * 
 * @since 4.3.0
 */
public interface SourcePolicyResult {

  /**
   * @return the event that was obtained as a result of the policy chain
   */
  CoreEvent getResult();
}
