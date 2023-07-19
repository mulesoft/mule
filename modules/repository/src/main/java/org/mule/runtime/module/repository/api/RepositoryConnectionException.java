/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.repository.api;

/**
 * Exception thrown when the repository system could not connect to an external repository
 *
 * @since 4.0
 */
public final class RepositoryConnectionException extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public RepositoryConnectionException(String message, Throwable cause) {
    super(message, cause);
  }
}
