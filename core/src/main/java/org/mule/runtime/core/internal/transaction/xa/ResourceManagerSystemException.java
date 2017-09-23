/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.transaction.xa;

import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.core.api.transaction.xa.ResourceManagerException;

public class ResourceManagerSystemException extends ResourceManagerException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 1202058044460490597L;

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
