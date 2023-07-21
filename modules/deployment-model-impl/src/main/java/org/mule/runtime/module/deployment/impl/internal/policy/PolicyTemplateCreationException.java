/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
