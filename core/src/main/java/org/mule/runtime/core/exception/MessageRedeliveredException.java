/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.exception;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.api.i18n.I18nMessage;

public class MessageRedeliveredException extends MessagingException {

  /**
   * Serial version
   */
  private static final long serialVersionUID = 9013890402770563931L;

  String messageId;
  int redeliveryCount;
  int maxRedelivery;

  protected MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, Event event,
                                        I18nMessage message) {
    super(message, event);
    this.messageId = messageId;
    this.redeliveryCount = redeliveryCount;
    this.maxRedelivery = maxRedelivery;
  }

  public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, Event event,
                                     I18nMessage message,
                                     Processor failingMessageProcessor) {
    super(message, event, failingMessageProcessor);
    this.messageId = messageId;
    this.redeliveryCount = redeliveryCount;
    this.maxRedelivery = maxRedelivery;
  }

  public MessageRedeliveredException(String messageId, int redeliveryCount, int maxRedelivery, Event event,
                                     Processor failingMessageProcessor) {
    this(messageId, redeliveryCount, maxRedelivery, event,
         CoreMessages.createStaticMessage("Maximum redelivery attempts reached"), failingMessageProcessor);
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
