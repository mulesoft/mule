/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import org.mule.runtime.api.exception.ErrorMessageAwareException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.runtime.api.message.Message;

/**
 * An exception that occurred while transforming a message, thrown by {@link MessageTransformer}s.
 *
 * @since 4.0
 */
public class MessageTransformerException extends MuleException implements ErrorMessageAwareException {

  private transient Transformer transformer;
  private final Message message;

  public MessageTransformerException(I18nMessage message, Transformer transformer,
                                     Message muleMessage) {
    super(message);
    this.transformer = transformer;
    this.message = muleMessage;
  }

  public MessageTransformerException(I18nMessage message, Transformer transformer, Throwable cause,
                                     Message muleMessage) {
    super(message, cause);
    this.transformer = transformer;
    this.message = muleMessage;
  }

  public MessageTransformerException(Transformer transformer, Throwable cause, Message message) {
    super(cause);
    this.transformer = transformer;
    this.message = message;
  }

  public Transformer getTransformer() {
    return transformer;
  }

  @Override
  public Message getErrorMessage() {
    return message;
  }

}
