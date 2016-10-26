/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;

/**
 * Exception to signal that too many configs are eligible for executing a component and thus the user should manually select one
 *
 * @since 4.0
 */
public class TooManyConfigsException extends MuleRuntimeException {

  private final ExtensionModel extensionModel;
  private final int configsCount;

  public TooManyConfigsException(String message, ExtensionModel extensionModel, int configsCount) {
    super(createStaticMessage(message));
    this.extensionModel = extensionModel;
    this.configsCount = configsCount;
  }

  public ExtensionModel getExtensionModel() {
    return extensionModel;
  }

  public int getConfigsCount() {
    return configsCount;
  }
}

