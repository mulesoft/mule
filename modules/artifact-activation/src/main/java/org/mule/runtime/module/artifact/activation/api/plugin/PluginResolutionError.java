/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.artifact.activation.api.plugin;

/**
 * Thrown to indicate that a given set of plugins was not resolved.
 * 
 * @since 4.5
 */
public class PluginResolutionError extends RuntimeException {

  private static final long serialVersionUID = -8044444668416261423L;

  /**
   * {@inheritDoc}
   */
  public PluginResolutionError(String message) {
    super(message);
  }
}
