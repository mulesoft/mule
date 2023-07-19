/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 *
 */
public final class DeploymentInitException extends DeploymentException {

  public DeploymentInitException(I18nMessage message) {
    super(message);
  }

  public DeploymentInitException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
