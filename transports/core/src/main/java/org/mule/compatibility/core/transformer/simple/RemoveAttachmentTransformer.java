/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.util.AttributeEvaluator;
import org.mule.runtime.core.util.WildcardAttributeEvaluator;

import java.nio.charset.Charset;

public class RemoveAttachmentTransformer extends AbstractMessageTransformer {

  private AttributeEvaluator nameEvaluator;
  private WildcardAttributeEvaluator wildcardAttributeEvaluator;

  public RemoveAttachmentTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    nameEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    MuleMessage message = event.getMessage();
    try {
      if (wildcardAttributeEvaluator.hasWildcards()) {
        final Builder builder = MuleMessage.builder(event.getMessage());
        wildcardAttributeEvaluator.processValues(message.getOutboundAttachmentNames(),
                                                 matchedValue -> builder.removeOutboundAttachment(matchedValue));
        event.setMessage(builder.build());
      } else {
        Object keyValue = nameEvaluator.resolveValue(event);
        if (keyValue != null) {
          event.setMessage(MuleMessage.builder(event.getMessage()).removeOutboundAttachment(keyValue.toString()).build());
        } else {
          logger.info("Attachment key expression return null, no attachment will be removed");
        }
      }
      return message;
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    RemoveAttachmentTransformer clone = (RemoveAttachmentTransformer) super.clone();
    clone.setAttachmentName(this.nameEvaluator.getRawValue());
    return clone;
  }

  public void setAttachmentName(String attachmentName) {
    this.nameEvaluator = new AttributeEvaluator(attachmentName);
    this.wildcardAttributeEvaluator = new WildcardAttributeEvaluator(attachmentName);
  }

}
