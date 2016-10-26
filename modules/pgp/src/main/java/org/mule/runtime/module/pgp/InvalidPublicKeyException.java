/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.pgp;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessage;

public class InvalidPublicKeyException extends MuleRuntimeException {

  private static final long serialVersionUID = -6015475303289155166L;

  public InvalidPublicKeyException(I18nMessage message) {
    super(message);
  }
}


