/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.runtime.api.message.NullAttributes.NULL_ATTRIBUTES;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.core.api.util.StreamingUtils.streamingContent;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageIterator;
import static org.mule.runtime.extension.internal.loader.enricher.MimeTypeParametersDeclarationEnricher.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.extension.internal.loader.enricher.MimeTypeParametersDeclarationEnricher.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.util.message.MessageUtils;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.internal.runtime.ExecutionContextAdapter;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.Optional;

/**
 * Base class for {@link ReturnDelegate} implementations.
 * <p/>
 * Contains the logic for taking an operation's output value and turn it into a {@link Message} which not only contains the
 * updated payload but also the proper {@link DataType} and attributes.
 * <p>
 * It also consider the case in which the value is a {@code List<Result>} which should be turned into a {@code List<Message>}.
 * For any of this cases, it also allows specifying a {@link CursorProviderFactory} which will transform the streaming payload
 * values into {@link CursorProvider} instances. As said before, this is also applied then the value is a message or list of
 * them
 *
 * @since 4.0
 */
abstract class AbstractReturnDelegate implements ReturnDelegate {

  protected final MuleContext muleContext;
  private final boolean returnsListOfMessages;
  private final CursorProviderFactory cursorProviderFactory;

  /**
   * Creates a new instance
   *
   * @param componentModel        the component which produces the return value
   * @param cursorProviderFactory the {@link CursorProviderFactory} to use when a message is doing cursor based streaming. Can be {@code null}
   * @param muleContext           the {@link MuleContext} of the owning application
   */
  protected AbstractReturnDelegate(ComponentModel componentModel,
                                   CursorProviderFactory cursorProviderFactory,
                                   MuleContext muleContext) {
    returnsListOfMessages = returnsListOfMessages(componentModel);
    this.muleContext = muleContext;
    this.cursorProviderFactory = cursorProviderFactory;
  }

  protected Message toMessage(Object value, ExecutionContextAdapter operationContext) {
    final MediaType mediaType = resolveMediaType(value, operationContext);
    final Event event = operationContext.getEvent();

    if (value instanceof Result) {
      return MessageUtils.toMessage((Result) value, mediaType, cursorProviderFactory, event);
    } else {
      if (value instanceof Collection && returnsListOfMessages) {
        value = toMessageCollection((Collection<Result>) value, cursorProviderFactory, event);
      } else if (value instanceof Iterator && returnsListOfMessages) {
        value = toMessageIterator((Iterator<Result>) value, cursorProviderFactory, event);
      }
      return Message.builder()
          .payload(streamingContent(value, cursorProviderFactory, event))
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
