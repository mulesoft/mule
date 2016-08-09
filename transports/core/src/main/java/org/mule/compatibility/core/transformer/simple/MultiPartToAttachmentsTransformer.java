/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.core.transformer.simple;

import static org.mule.runtime.core.util.IOUtils.toDataHandler;

import org.mule.runtime.api.message.MultiPartPayload;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleMessage;
import org.mule.runtime.core.api.MuleMessage.Builder;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.message.DefaultMultiPartPayload;
import org.mule.runtime.core.message.PartAttributes;
import org.mule.runtime.core.transformer.AbstractMessageTransformer;

import java.nio.charset.Charset;

/**
 * Transforms the message, putting all attachment parts of the {@link DefaultMultiPartPayload} form the source as outbound
 * attachments in the returning message. of the returning message.
 *
 * @since 4.0
 */
public class MultiPartToAttachmentsTransformer extends AbstractMessageTransformer {

  public MultiPartToAttachmentsTransformer() {
    registerSourceType(DataType.builder().type(MultiPartPayload.class).build());
    setReturnDataType(DataType.OBJECT);
  }

  @Override
  public Object transformMessage(MuleEvent event, Charset outputEncoding) throws TransformerException {
    final MuleMessage message = event.getMessage();
    try {
      final Builder builder = MuleMessage.builder(message);

      final DefaultMultiPartPayload multiPartPayload = (DefaultMultiPartPayload) message.getPayload();
      if (multiPartPayload.hasBodyPart()) {
        builder.payload(multiPartPayload.getBodyPart().getPayload());
      } else {
        builder.nullPayload();
      }

      for (org.mule.runtime.api.message.MuleMessage muleMessage : multiPartPayload.getNonBodyParts()) {
        final PartAttributes attributes = (PartAttributes) muleMessage.getAttributes();
        builder.addOutboundAttachment(attributes.getName(), toDataHandler(attributes.getName(), muleMessage.getPayload(),
                                                                          muleMessage.getDataType().getMediaType()));
      }

      event.setMessage(builder.build());
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
    return event.getMessage();
  }
}
