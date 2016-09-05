/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.processor.simple;

import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.simple.SimpleMessageProcessor;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoveAttachmentProcessor extends SimpleMessageProcessor {

  private static final Logger logger = LoggerFactory.getLogger(RemoveAttachmentProcessor.class);

  private AttributeEvaluator nameEvaluator;
  private WildcardAttributeEvaluator wildcardAttributeEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    nameEvaluator.initialize(muleContext.getExpressionLanguage());
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    MuleMessage message = event.getMessage();
    try {
      if (wildcardAttributeEvaluator.hasWildcards()) {
        final Builder builder = MuleMessage.builder(event.getMessage());
        wildcardAttributeEvaluator.processValues(message.getOutboundAttachmentNames(),
                                                 matchedValue -> builder.removeOutboundAttachment(matchedValue));
        return MuleEvent.builder(event).message(builder.build()).build();
      } else {
        Object keyValue = nameEvaluator.resolveValue(event);
        if (keyValue != null) {
          return MuleEvent.builder(event)
              .message(MuleMessage.builder(event.getMessage()).removeOutboundAttachment(keyValue.toString()).build()).build();
        } else {
          logger.info("Attachment key expression return null, no attachment will be removed");
        }
      }
      return event;
    } catch (Exception e) {
      throw new MessagingException(event, e);
    }
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    RemoveAttachmentProcessor clone = (RemoveAttachmentProcessor) super.clone();
    clone.setAttachmentName(this.nameEvaluator.getRawValue());
    return clone;
  }

  public void setAttachmentName(String attachmentName) {
    this.nameEvaluator = new AttributeEvaluator(attachmentName);
    this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(attachmentName);
  }
}
