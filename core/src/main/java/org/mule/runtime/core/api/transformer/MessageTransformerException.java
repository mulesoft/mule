/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.transformer;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

/**
 * An exception that occurred while transforming a message, thrown by {@link MessageTransformer}s.
 *
 * @since 4.0
 */
public class MessageTransformerException extends MuleException {

  private transient Transformer transformer;

  public MessageTransformerException(I18nMessage message, Transformer transformer) {
    super(message);
    this.transformer = transformer;
  }

  public MessageTransformerException(I18nMessage message, Transformer transformer, Throwable cause) {
    super(message, cause);
    this.transformer = transformer;
  }

  public MessageTransformerException(Transformer transformer, Throwable cause) {
    super(cause);
    this.transformer = transformer;
  }

  public Transformer getTransformer() {
    return transformer;
  }
}
