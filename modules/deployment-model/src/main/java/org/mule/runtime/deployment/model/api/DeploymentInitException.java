/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
