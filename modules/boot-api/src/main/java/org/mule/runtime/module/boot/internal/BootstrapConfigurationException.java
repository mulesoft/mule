/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.boot.internal;

/**
 * Allows for modeling a situation in which the bootstrapping should not continue and the bootstrapping application should exit.
 *
 * @since 4.5
 */
public class BootstrapConfigurationException extends Exception {

  private static final long serialVersionUID = 1L;

  private final int exitCode;

  public BootstrapConfigurationException(int exitCode, Throwable cause) {
    super(cause);
    this.exitCode = exitCode;
  }

  public BootstrapConfigurationException(int exitCode, String message, Throwable cause) {
    super(message, cause);
    this.exitCode = exitCode;
  }

  public BootstrapConfigurationException(int exitCode) {
    this.exitCode = exitCode;
  }

  /**
   * @return The exit code to use for the termination of the bootstrapping application.
   */
  public int getExitCode() {
    return exitCode;
  }
}
