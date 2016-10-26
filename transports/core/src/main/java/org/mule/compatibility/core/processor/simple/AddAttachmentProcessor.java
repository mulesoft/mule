/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.processor.simple;

import static org.mule.runtime.core.util.IOUtils.toDataHandler;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.exception.MessagingException;
import org.mule.runtime.core.processor.simple.SimpleMessageProcessor;
import org.mule.runtime.core.util.AttributeEvaluator;

import java.text.MessageFormat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AddAttachmentProcessor extends SimpleMessageProcessor {

  private static final Logger logger = LoggerFactory.getLogger(AddAttachmentProcessor.class);

  private AttributeEvaluator nameEvaluator;
  private AttributeEvaluator valueEvaluator;
  private AttributeEvaluator contentTypeEvaluator;

  @Override
  public void initialise() throws InitialisationException {
    nameEvaluator.initialize(muleContext.getExpressionLanguage());
    valueEvaluator.initialize(muleContext.getExpressionLanguage());
    contentTypeEvaluator.initialize(muleContext.getExpressionLanguage());
  }

  @Override
  public Event process(Event event) throws MuleException {
    try {
      Object keyValue = nameEvaluator.resolveValue(event);
      if (keyValue == null) {
        logger.error("Setting Null attachment key is not supported, this entry is being ignored");
      } else {
        String key = keyValue.toString();
        Object value = valueEvaluator.resolveValue(event);
        if (value == null) {
          logger.error(MessageFormat.format(
                                            "Attachment with key ''{0}'', not found on message using ''{1}''. Since the value was marked optional, nothing was set on the message for this attachment",
                                            key, valueEvaluator.getRawValue()));
        } else {
          MediaType contentType =
              DataType.builder().mediaType(contentTypeEvaluator.resolveStringValue(event)).build().getMediaType();
          return Event.builder(event).message(InternalMessage.builder(event.getMessage())
              .addOutboundAttachment(key, toDataHandler(key, value, contentType)).build()).build();
        }
      }

      return event;
    } catch (Exception e) {
      throw new MessagingException(event, e);
    }
  }

  public void setAttachmentName(String attachmentName) {
    this.nameEvaluator = new AttributeEvaluator(attachmentName);
  }

  public void setValue(String value) {
    this.valueEvaluator = new AttributeEvaluator(value);
  }

  public void setContentType(String contentType) {
    this.contentTypeEvaluator = new AttributeEvaluator(contentType);
  }
}
