/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
