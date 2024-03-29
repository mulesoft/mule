/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
