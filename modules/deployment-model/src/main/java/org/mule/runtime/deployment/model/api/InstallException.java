/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.deployment.model.api;

import org.mule.runtime.api.i18n.I18nMessage;

/**
 *
 */
public final class InstallException extends DeploymentException {

  public InstallException(I18nMessage message) {
    super(message);
  }

  public InstallException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
