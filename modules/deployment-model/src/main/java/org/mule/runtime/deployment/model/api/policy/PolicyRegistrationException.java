/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.policy;

/**
 * Thrown to indicate that an error was found while attempting to add a policy instance to an application.
 */
public final class PolicyRegistrationException extends Exception {

  /**
   * {@inheritDoc}
   */
  public PolicyRegistrationException(String message, Throwable cause) {
    super(message, cause);
  }

}
