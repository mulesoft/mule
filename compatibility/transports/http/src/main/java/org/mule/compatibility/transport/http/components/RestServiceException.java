/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.http.components;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class RestServiceException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = -1026055907767407434L;

  public RestServiceException(I18nMessage message) {
    super(message);
  }

  public RestServiceException(I18nMessage message, Throwable cause) {
    super(message, cause);
  }
}
