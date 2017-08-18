/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.exception;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.i18n.I18nMessage;

public class MessageRedeliveredException extends MuleException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 9013890402770563931L;

  String messageId;
  int redeliveryCount;
  int maxRedelivery;

  protected MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, I18nMessage message) {
    super(message);
    this.messageId = messageId;
    this.redeliveryCount = redeliveryCount;
    this.maxRedelivery = maxRedelivery;
  }

  public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery) {
    this(messageId, redeliveryCount, maxRedelivery, createStaticMessage("Maximum redelivery attempts reached"));
  }

  public String getMessageId() {
    return messageId;
  }

  public int getRedeliveryCount() {
    return redeliveryCount;
  }

  public int getMaxRedelivery() {
    return maxRedelivery;
  }
}
