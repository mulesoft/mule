/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.globalconfig.api.exception;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

import java.io.IOException;

/**
 * Exception thrown when there's a problem in the global runtime configuration.
 * 
 * @since 4.0
 */
public final class RuntimeGlobalConfigException extends MuleRuntimeException {

  public RuntimeGlobalConfigException(I18nMessage message) {
    super(message);
  }

  public RuntimeGlobalConfigException(Exception e) {
    super(e);
  }

  public RuntimeGlobalConfigException(I18nMessage message, IOException e) {
    super(message, e);
  }
}
