/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.ast.api.ComponentAst;

/**
 * Exception thrown when a requested {@link ComponentAst} in the configuration does not exists.
 */
public class NoSuchComponentModelException extends MuleRuntimeException {

  /**
   * Creates a new instance.
   *
   * @param message the exception message.
   */
  public NoSuchComponentModelException(I18nMessage message) {
    super(message);
  }

}
