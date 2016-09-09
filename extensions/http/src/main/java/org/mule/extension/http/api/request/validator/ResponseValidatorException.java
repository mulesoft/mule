/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.http.api.request.validator;

import static org.mule.runtime.core.config.i18n.I18nMessageFactory.createStaticMessage;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.config.i18n.CoreMessages;

/**
 * Signals that an error occurred while validating a {@link Message}
 *
 * @since 4.0
 */
public class ResponseValidatorException extends MuleException {

  public ResponseValidatorException(String message) {
    super(createStaticMessage(message));
  }
}
