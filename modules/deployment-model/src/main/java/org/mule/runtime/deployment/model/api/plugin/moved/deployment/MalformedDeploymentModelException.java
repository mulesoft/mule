/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api.plugin.moved.deployment;

/**
 * Typed exception thrown when creating a DeploymentModel if it's malformed.
 *
 * @since 4.0
 */
public class MalformedDeploymentModelException extends Exception {

  public MalformedDeploymentModelException(String message, Throwable cause) {
    super(message, cause);
  }

  public MalformedDeploymentModelException(String message) {
    super(message);
  }
}
