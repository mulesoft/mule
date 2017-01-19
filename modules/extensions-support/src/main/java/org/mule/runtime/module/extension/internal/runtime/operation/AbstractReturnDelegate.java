/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.core.util.message.MessageUtils.valueOrStreamProvider;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;
import static org.mule.runtime.core.util.message.MessageUtils.toMessageCollection;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.streaming.CursorStreamProvider;
import org.mule.runtime.core.api.streaming.bytes.CursorStreamProviderFactory;
import org.mule.runtime.core.util.message.MessageUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Optional;

/**
 * Base class for {@link ReturnDelegate} implementations.
 * <p/>
 * Contains the logic for taking an operation's output value and turn it into a {@link Message} which not only contains the
 * updated payload but also the proper {@link DataType} and attributes.
 * <p>
 * It also consider the case in which the value is a {@code List<Result>} which should be turned into a {@code List<Message>}.
 * For any of this cases, it also allows specifying a {@link CursorStreamProviderFactory} which will transform {@link InputStream}
 * values into {@link CursorStreamProvider} instances. As said before, this is also applied then the value is a message or list of
 * them
 *
 * @since 4.0
 */
abstract class AbstractReturnDelegate implements ReturnDelegate {

  protected final MuleContext muleContext;
  private final boolean returnsListOfMessages;
  private final CursorStreamProviderFactory cursorStreamProviderFactory;

  /**
   * Creates a new instance
   *
   * @param componentModel              the component which produces the return value
   * @param cursorStreamProviderFactory the {@link CursorStreamProviderFactory} to use when a message payload is an {@link InputStream}. Can be {@code null}
   * @param muleContext                 the {@link MuleContext} of the owning application
   */
  protected AbstractReturnDelegate(ComponentModel componentModel,
                                   CursorStreamProviderFactory cursorStreamProviderFactory,
                                   MuleContext muleContext) {
    returnsListOfMessages = returnsListOfMessages(componentModel);
    this.muleContext = muleContext;
    this.cursorStreamProviderFactory = cursorStreamProviderFactory;
  }

  protected Message toMessage(Object value, ExecutionContextAdapter operationContext) {
    final MediaType mediaType = resolveMediaType(value, operationContext);
    final Event event = operationContext.getEvent();

    if (value instanceof Result) {
      return MessageUtils.toMessage((Result) value, mediaType, cursorStreamProviderFactory, event);
    } else {
      if (value instanceof Collection && returnsListOfMessages) {
        value = toMessageCollection((Collection<Result>) value, mediaType, cursorStreamProviderFactory, event);
      }
      return Message.builder()
          .payload(valueOrStreamProvider(value, cursorStreamProviderFactory, event).getValue().orElse(null))
          .mediaType(mediaType)
          .attributes(NULL_ATTRIBUTES).build();
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
  private MediaType resolveMediaType(Object value, ExecutionContextAdapter<ComponentModel> operationContext) {
    Charset existingEncoding = getDefaultEncoding(muleContext);
    MediaType mediaType = null;
    if (value instanceof Result) {
      final Optional<MediaType> optionalMediaType = ((Result) value).getMediaType();
      if (optionalMediaType.isPresent()) {
        mediaType = optionalMediaType.get();
        if (mediaType.getCharset().isPresent()) {
          existingEncoding = mediaType.getCharset().get();
        }
      }
    }

    if (mediaType == null) {
      mediaType = ANY;
    }

    if (operationContext.hasParameter(MIME_TYPE_PARAMETER_NAME)) {
      mediaType = MediaType.parse(operationContext.getParameter(MIME_TYPE_PARAMETER_NAME));
    }

    if (operationContext.hasParameter(ENCODING_PARAMETER_NAME)) {
      mediaType =
          mediaType.withCharset(Charset.forName(operationContext.getParameter(ENCODING_PARAMETER_NAME)));
    } else {
      mediaType = mediaType.withCharset(existingEncoding);
    }

    return mediaType;
  }
}
