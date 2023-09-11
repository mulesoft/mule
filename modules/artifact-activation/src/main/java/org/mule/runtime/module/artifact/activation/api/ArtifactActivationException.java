/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.activation.api;

import org.mule.api.annotation.NoExtend;
import org.mule.api.annotation.NoInstantiate;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * Indicates that an error occurred while operating on an artifact.
 *
 * @since 4.5
 */
@NoExtend
@NoInstantiate
public class ArtifactActivationException extends MuleRuntimeException {

  private static final long serialVersionUID = 1402222804782983837L;

  /**
   * @param message the exception message
   */
  public ArtifactActivationException(I18nMessage message) {
    super(message);
  }

  /**
   * @param message the exception message
   * @param cause   the exception that triggered this exception
   */
  public ArtifactActivationException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
