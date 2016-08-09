/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import static org.mule.runtime.core.message.DefaultMultiPartPayload.BODY_ATTRIBUTES;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import javax.activation.DataHandler;

/**
 * Transforms the message, putting all inbound attachments of the source message as parts in a {@link DefaultMultiPartPayload} of
 * the returning message.
 *
 * @since 4.0
 */
public class AttachmentsToMultiPartTransformer extends AbstractMessageTransformer {

  public AttachmentsToMultiPartTransformer() {
    registerSourceType(DataType.OBJECT);
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    final MuleMessage message = event.getMessage();
    try {
      final Builder builder = MuleMessage.builder(message);

      List<org.mule.runtime.api.message.MuleMessage> parts = new ArrayList<>();

      if (message.getPayload() != null) {
        parts.add(MuleMessage.builder().payload(message.getPayload()).attributes(BODY_ATTRIBUTES).build());
      }

      for (String attachmentName : message.getInboundAttachmentNames()) {
        DataHandler attachment = message.getInboundAttachment(attachmentName);

        parts.add(MuleMessage.builder().payload(attachment.getInputStream())
            .mediaType(MediaType.parse(attachment.getContentType())).attributes(new PartAttributes(attachmentName)).build());
      }

      event.setMessage(builder.payload(new DefaultMultiPartPayload(parts)).build());
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
    return event.getMessage();
  }
}
