/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.core.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.message.MuleMessage;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.OperationResult;
import org.mule.runtime.module.extension.internal.runtime.OperationContextAdapter;

import java.nio.charset.Charset;
import java.util.Optional;

/**
 * Base class for {@link ReturnDelegate} implementations.
 * <p/>
 * Contains the logic for taking an operation's output value and turn it into a {@link MuleMessage} which not only contains the
 * updated payload but also the proper {@link DataType} and attributes.
 *
 * @since 4.0
 */
abstract class AbstractReturnDelegate implements ReturnDelegate {

  protected final MuleContext muleContext;

  /**
   * Creates a new instance
   *
   * @param muleContext the {@link MuleContext} of the owning application
   */
  protected AbstractReturnDelegate(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  protected MuleMessage toMessage(Object value, OperationContextAdapter operationContext) {
    MediaType mediaType = resolveMediaType(value, operationContext);
    if (value instanceof OperationResult) {
      OperationResult operationResult = (OperationResult) value;
      return MuleMessage.builder().payload(operationResult.getOutput()).mediaType(mediaType)
          .attributes((Attributes) operationResult.getAttributes().orElse(NULL_ATTRIBUTES)).build();
    } else {
      return MuleMessage.builder().payload(value).mediaType(mediaType).attributes(NULL_ATTRIBUTES).build();
    }
  }

  /**
   * If provided, mimeType and encoding configured as operation parameters will take precedence over what comes with the message's
   * {@link DataType}.
   * 
   * @param value
   * @param operationContext
   * @return
   */
  private MediaType resolveMediaType(Object value, OperationContextAdapter operationContext) {
    Charset existingEncoding = getDefaultEncoding(muleContext);
    MediaType mediaType = null;
    if (value instanceof OperationResult) {
      final Optional<MediaType> optionalMediaType = ((OperationResult) value).getMediaType();
      if (optionalMediaType.isPresent()) {
        mediaType = optionalMediaType.get();
        if (mediaType.getCharset().isPresent()) {
          existingEncoding = mediaType.getCharset().get();
        }
      }
    }

    if (mediaType == null) {
      mediaType = MediaType.ANY;
    }

    if (operationContext.hasParameter(MIME_TYPE_PARAMETER_NAME)) {
      mediaType = MediaType.parse(operationContext.getTypeSafeParameter(MIME_TYPE_PARAMETER_NAME, String.class));
    }

    if (operationContext.hasParameter(ENCODING_PARAMETER_NAME)) {
      mediaType =
          mediaType.withCharset(Charset.forName(operationContext.getTypeSafeParameter(ENCODING_PARAMETER_NAME, String.class)));
    } else {
      mediaType = mediaType.withCharset(existingEncoding);
    }

    return mediaType;
  }
}
