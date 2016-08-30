/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.http.internal.request;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.config.i18n.CoreMessages;

public class ResponseValidatorException extends MessagingException {

  public ResponseValidatorException(String message, MuleEvent event) {
    super(CoreMessages.createStaticMessage(message), event);
  }
}
