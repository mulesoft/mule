/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transaction.xa;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class ResourceManagerException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -2710661653314559260L;

  /**
   * 
   */
  public ResourceManagerException() {
    super();
  }

  /**
   * @param message
   */
  public ResourceManagerException(I18nMessage message) {
    super(message);
  }

  /**
   * @param cause
   */
  public ResourceManagerException(Throwable cause) {
    super(cause);
  }

  /**
   * @param message
   * @param cause
   */
  public ResourceManagerException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }

}
