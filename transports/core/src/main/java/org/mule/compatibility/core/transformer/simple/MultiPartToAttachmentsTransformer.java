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
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.message.InternalMessage;
import org.mule.runtime.core.api.message.InternalMessage.Builder;
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
  public Object transformMessage(Event event, Charset outputEncoding) throws TransformerException {
    final InternalMessage message = event.getMessage();
    try {
      final Builder builder = InternalMessage.builder(message);

      final DefaultMultiPartPayload multiPartPayload = (DefaultMultiPartPayload) message.getPayload().getValue();
      if (multiPartPayload.hasBodyPart()) {
        builder.payload(multiPartPayload.getBodyPart().getPayload().getValue());
      } else {
        builder.nullPayload();
      }

      for (org.mule.runtime.api.message.Message muleMessage : multiPartPayload.getNonBodyParts()) {
        final PartAttributes attributes = (PartAttributes) muleMessage.getAttributes();
        builder.addOutboundAttachment(attributes.getName(),
                                      toDataHandler(attributes.getName(), muleMessage.getPayload().getValue(),
                                                    muleMessage.getPayload().getDataType().getMediaType()));
      }

      return builder.build();
    } catch (Exception e) {
      throw new TransformerException(this, e);
    }
  }
}
