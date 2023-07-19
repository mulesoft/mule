/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.manager.jdk;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Signals that an {@link ExtensionModel} does not support the Java version that Mule is running on
 *
 * @since 4.5.0
 */
public class JavaVersionNotSupportedByExtensionException extends MuleRuntimeException {

  public JavaVersionNotSupportedByExtensionException(String message) {
    super(createStaticMessage(message));
  }
}
