/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.dsl.model;

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
