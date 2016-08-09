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

public class CopyAttachmentsTransformer extends AbstractMessageTransformer {

  private AttributeEvaluator attachmentNameEvaluator;
  private WildcardAttributeEvaluator wildcardAttachmentNameEvaluator;

  public CopyAttachmentsTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public void initialise() throws InitialisationException {
    super.initialise();
    this.attachmentNameEvaluator.initialize(muleContext.getExpressionManager());
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    final MuleMessage message = event.getMessage();
    try {
      if (wildcardAttachmentNameEvaluator.hasWildcards()) {
        final Builder builder = MuleMessage.builder(message);
        wildcardAttachmentNameEvaluator.processValues(message.getInboundAttachmentNames(), matchedValue -> builder
            .addOutboundAttachment(matchedValue, message.getInboundAttachment(matchedValue)));
        event.setMessage(builder.build());
      } else {
        String attachmentName = attachmentNameEvaluator.resolveValue(event).toString();
        event.setMessage(MuleMessage.builder(message)
            .addOutboundAttachment(attachmentName, message.getInboundAttachment(attachmentName)).build());
      }
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
    return event.getMessage();
  }

  @Override
  public Object clone() throws CloneNotSupportedException {
    CopyAttachmentsTransformer clone = (CopyAttachmentsTransformer) super.clone();
    clone.setAttachmentName(this.attachmentNameEvaluator.getRawValue());
    return clone;
  }

  public void setAttachmentName(String attachmentName) {
    this.attachmentNameEvaluator = new AttributeEvaluator(attachmentName);
    this.wildcardAttachmentNameEvaluator = new WildcardAttributeEvaluator(attachmentName);
  }

}
