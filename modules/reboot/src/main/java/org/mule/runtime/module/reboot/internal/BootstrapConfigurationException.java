/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.reboot.internal;

/**
 * Allows for modeling a situation in which the bootstrapping should not continue and the bootstrapping application should exit.
 *
 * @since 4.5
 */
public class BootstrapConfigurationException extends Exception {

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
