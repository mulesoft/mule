/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.deployment.model.api;

import org.mule.api.annotation.NoExtend;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

@NoExtend
public class DeploymentException extends MuleRuntimeException {

  public DeploymentException(I18nMessage message) {
    super(message);
  }

  public DeploymentException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
