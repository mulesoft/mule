/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.transaction.xa;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

public class ResourceManagerSystemException extends ResourceManagerException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1202058044460490599L;

  /**
   * @param message
   */
  public ResourceManagerSystemException(I18nMessage message) {
    super(message);
  }

  /**
   * @param cause
   */
  public ResourceManagerSystemException(Throwable cause) {
    super(cause);
  }

}
