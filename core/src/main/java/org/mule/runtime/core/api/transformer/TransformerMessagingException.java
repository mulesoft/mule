/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.config.i18n.Message;

/**
 * An exception that occurred while transforming a message.
 */
public class TransformerMessagingException extends MessagingException {

  private transient Transformer transformer;

  public TransformerMessagingException(Message message, MuleEvent event, Transformer transformer) {
    super(message, event, transformer);
    this.transformer = transformer;
  }

  public TransformerMessagingException(Message message, MuleEvent event, Transformer transformer, Throwable cause) {
    super(message, event, cause, transformer);
    this.transformer = transformer;
  }

  public TransformerMessagingException(MuleEvent event, Transformer transformer, Throwable cause) {
    super(event, cause, transformer);
    this.transformer = transformer;
  }

  public Transformer getTransformer() {
    return transformer;
  }
}
