/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
