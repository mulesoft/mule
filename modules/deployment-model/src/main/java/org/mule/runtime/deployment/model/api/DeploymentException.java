/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
