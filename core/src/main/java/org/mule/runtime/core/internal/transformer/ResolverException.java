/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transformer;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.transformer.TransformerException;

/**
 * An exception that is thrown by resolver classes responsible for finding objects in the registry based on particular criteria
 */
public class ResolverException extends TransformerException {

  private static final long serialVersionUID = 389248508135507438L;

  public ResolverException(I18nMessage message) {
    super(message);
  }

  public ResolverException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
