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

public class CopyAttachmentsProcessor extends SimpleMessageProcessor {

  private AttributeEvaluator attachmentNameEvaluator;
  private WildcardAttributeEvaluator wildcardAttachmentNameEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    this.attachmentNameEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public MuleEvent process(MuleEvent event) throws MuleException {
    final MuleMessage message = event.getMessage();
    try {
      if (wildcardAttachmentNameEvaluator.hasWildcards()) {
        final Builder builder = MuleMessage.builder(message);
        wildcardAttachmentNameEvaluator.processValues(message.getInboundAttachmentNames(), matchedValue -> builder
            .addOutboundAttachment(matchedValue, message.getInboundAttachment(matchedValue)));
        return MuleEvent.builder(event).message(builder.build()).build();
      } else {
        String attachmentName = attachmentNameEvaluator.resolveValue(event).toString();
        return MuleEvent.builder(event).message(MuleMessage.builder(message)
            .addOutboundAttachment(attachmentName, message.getInboundAttachment(attachmentName)).build()).build();
      }
    } catch (Exception e) {
      throw new MessagingException(event, e);
    }
  }

  public void setAttachmentName(String attachmentName) {
    this.attachmentNameEvaluator = new AttributeEvaluator(attachmentName);
    this.wildcardAttachmentNameEvaluator = new WildcardAttributeEvaluator(attachmentName);
  }
}
