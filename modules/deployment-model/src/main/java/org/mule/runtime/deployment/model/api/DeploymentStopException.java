/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 *
 */
public final class DeploymentStopException extends DeploymentException {

  public DeploymentStopException(I18nMessage message) {
    super(message);
  }

  public DeploymentStopException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
