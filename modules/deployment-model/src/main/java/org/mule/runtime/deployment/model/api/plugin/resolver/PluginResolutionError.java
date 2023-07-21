/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api.plugin.resolver;

/**
 * Thrown to indicate that a given set of plugins was not resolved.
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
