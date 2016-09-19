/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.plugin;

/**
 * Thrown to indicate that a given set of plugins was not resolved.
 */
public class PluginResolutionError extends RuntimeException {

  /**
   * {@inheritDoc}
   */
  public PluginResolutionError(String message) {
    super(message);
  }
}
