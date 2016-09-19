/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.api.application;

/**
 * Indicates the status of a Mule application that corresponds to the last executed lifecycle phase on the application's
 * {@link org.mule.runtime.core.api.MuleContext}.
 */
public enum ApplicationStatus {
  CREATED,

  INITIALISED,

  STARTED,

  STOPPED,

  DEPLOYMENT_FAILED,

  DESTROYED
}
