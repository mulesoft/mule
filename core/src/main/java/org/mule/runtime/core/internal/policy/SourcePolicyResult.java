/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
