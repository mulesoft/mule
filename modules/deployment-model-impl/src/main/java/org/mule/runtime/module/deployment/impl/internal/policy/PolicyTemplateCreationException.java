/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.policy;

import org.mule.runtime.deployment.model.api.policy.PolicyTemplate;

/**
 * Thrown to indicate that an error was found while attempting to create a {@link PolicyTemplate} artifact.
 */
public class PolicyTemplateCreationException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public PolicyTemplateCreationException(String message, Throwable cause) {
    super(message, cause);
  }
}
