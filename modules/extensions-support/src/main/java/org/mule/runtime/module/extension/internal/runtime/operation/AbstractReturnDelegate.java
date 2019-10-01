/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static java.util.Optional.ofNullable;
import static org.apache.commons.io.IOUtils.EOF;
import static org.mule.runtime.api.metadata.MediaTypeUtils.parseCharset;
import static org.mule.runtime.core.api.util.StreamingUtils.supportsStreaming;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageCollection;
import static org.mule.runtime.core.internal.util.message.MessageUtils.toMessageIterator;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isJavaCollection;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.ReturnHandler.nullHandler;
import static org.mule.runtime.module.extension.internal.util.MediaTypeUtils.getDefaultMediaType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.ComponentModel;
import org.mule.runtime.api.meta.model.HasOutputModel;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.CursorProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.util.StreamingUtils;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultCollection;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultIterator;
import org.mule.runtime.core.internal.util.mediatype.PayloadMediaTypeResolver;
import org.mule.runtime.core.internal.util.message.MessageUtils;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.CollectionReturnHandler;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.MapReturnHandler;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.ReturnHandler;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.io.input.ClosedInputStream;
import org.apache.commons.io.input.ProxyInputStream;

/**
 * Base class for {@link ReturnDelegate} implementations.
 * <p/>
 * Contains the logic for taking an operation's output value and turn it into a {@link Message} which not only contains the
 * updated payload but also the proper {@link DataType} and attributes.
 * <p>
 * It also consider the case in which the value is a {@code List<Result>} which should be turned into a {@code List<Message>}. For
 * any of this cases, it also allows specifying a {@link CursorProviderFactory} which will transform the streaming payload values
 * into {@link CursorProvider} instances. As said before, this is also applied then the value is a message or list of them
 *
 * @since 4.0
 */
abstract class AbstractReturnDelegate implements ReturnDelegate {

  protected final MuleContext muleContext;
  private boolean returnsListOfMessages = false;
  private final CursorProviderFactory cursorProviderFactory;
  private final MediaType defaultMediaType;
  private boolean isSpecialHandling = false;
  private ReturnHandler returnHandler = nullHandler();

  private final Charset defaultEncoding;

  /**
   * Creates a new instance
   *
   * @param componentModel the component which produces the return value
   * @param cursorProviderFactory the {@link CursorProviderFactory} to use when a message is doing cursor based streaming. Can be
   *        {@code null}
   * @param muleContext the {@link MuleContext} of the owning application
   */
  protected AbstractReturnDelegate(ComponentModel componentModel,
                                   CursorProviderFactory cursorProviderFactory,
                                   MuleContext muleContext) {

    if (componentModel instanceof HasOutputModel) {
      HasOutputModel hasOutputModel = (HasOutputModel) componentModel;
      returnsListOfMessages = returnsListOfMessages(hasOutputModel);

      MetadataType outputType = hasOutputModel.getOutput().getType();

      if (isMap(outputType)) {
        isSpecialHandling = true;
        returnHandler = new MapReturnHandler(hasOutputModel);
      } else if (isJavaCollection(outputType)) {
        isSpecialHandling = true;
        returnHandler = new CollectionReturnHandler(outputType);
      }
    }

    this.muleContext = muleContext;
    this.cursorProviderFactory = cursorProviderFactory;

    defaultEncoding = getDefaultEncoding(muleContext);
    defaultMediaType = getDefaultMediaType(componentModel);
  }

  protected Message toMessage(Object value, ExecutionContextAdapter operationContext) {
    Map<String, Object> params = operationContext.getParameters();
    Optional<MediaType> contextMimeTypeParam = getContextMimeType(params);
    Optional<Charset> contextEncodingParam = getContextEncoding(params);
    final MediaType mediaType = resolveMediaType(value, contextMimeTypeParam, contextEncodingParam);
    final CoreEvent event = operationContext.getEvent();

    if (value instanceof Result) {
      Result resultValue = (Result) value;
      if (resultValue.getOutput() instanceof InputStream) {
        ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
        if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
          resultValue = resultValue.copy()
              .output(new ConnectedInputStreamWrapper((InputStream) resultValue.getOutput(), connectionHandler))
              .build();
        }
      }
      return isSpecialHandling && returnHandler.handles(resultValue.getOutput())
          ? MessageUtils.toMessage(resultValue, mediaType, cursorProviderFactory, event, returnHandler.getDataType())
          : MessageUtils.toMessage(resultValue, mediaType, cursorProviderFactory, event);
    } else {
      PayloadMediaTypeResolver payloadMediaTypeResolver = new PayloadMediaTypeResolver(getDefaultEncoding(muleContext),
                                                                                       defaultMediaType,
                                                                                       contextEncodingParam,
                                                                                       contextMimeTypeParam);
      if (value instanceof Collection && returnsListOfMessages) {
        value = toLazyMessageCollection((Collection<Result>) value, operationContext, cursorProviderFactory, mediaType, event);
        value = toMessageCollection(new MediaTypeDecoratedResultCollection((Collection<Result>) value, payloadMediaTypeResolver),
                                    cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext());
      } else if (value instanceof Iterator && returnsListOfMessages) {
        value = toMessageIterator(new MediaTypeDecoratedResultIterator((Iterator<Result>) value, payloadMediaTypeResolver),
                                  cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext());
      }

      value = streamingContent(value, operationContext, cursorProviderFactory,
                               ((BaseEventContext) event.getContext()).getRootContext());

      Message.Builder messageBuilder;

      // TODO MULE-13302: this doesn't completely makes sense. IT doesn't account for an Iterator<Message>
      // org.mule.runtime.api.metadata.DataType.MULE_MESSAGE_COLLECTION doesn't completely makes sense
      if (returnsListOfMessages && value instanceof Collection) {
        messageBuilder = Message.builder().collectionValue((Collection) value, Message.class);
      } else if (isSpecialHandling && returnHandler.handles(value)) {
        messageBuilder = returnHandler.toMessageBuilder(value);
      } else {
        messageBuilder = Message.builder().value(value);
      }

      return messageBuilder.mediaType(mediaType).build();
    }
  }


  private Collection<Object> toLazyMessageCollection(Collection<Result> values,
                                                     ExecutionContextAdapter operationContext,
                                                     CursorProviderFactory cursorProviderFactory,
                                                     MediaType mediaType,
                                                     CoreEvent event) {
    Collection<Object> lazyMessageCollection = new ArrayList<>();
    values.forEach(value -> {
      if (value.getOutput() instanceof InputStream) {
        ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
        if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
          value = value.copy()
              .output(new ConnectedInputStreamWrapper((InputStream) value.getOutput(), connectionHandler))
              .build();
        }

        Message message = MessageUtils.toMessage(value, mediaType, cursorProviderFactory, event);

        lazyMessageCollection.add(message);
      } else {
        lazyMessageCollection.add(value);
      }
    });
    return lazyMessageCollection;
  }

  private Optional<MediaType> getContextMimeType(Map<String, Object> params) {
    return ofNullable((String) params.get(MIME_TYPE_PARAMETER_NAME)).map(mimeType -> MediaType.parse(mimeType));
  }

  private Optional<Charset> getContextEncoding(Map<String, Object> params) {
    return ofNullable((String) params.get(ENCODING_PARAMETER_NAME)).map(encoding -> parseCharset(encoding));
  }

  private Object streamingContent(Object value,
                                  ExecutionContextAdapter operationContext,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext) {
    if (value instanceof InputStream) {
      ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
      if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
        value = new ConnectedInputStreamWrapper((InputStream) value, connectionHandler);
      }
    }

    return StreamingUtils.streamingContent(value, cursorProviderFactory, eventContext);
  }

  /**
   * If provided, mimeType and encoding configured as operation parameters will take precedence over what comes with the message's
   * {@link DataType}.
   *
   * @param value
   * @param operationContext
   * @return
   */
  protected MediaType resolveMediaType(Object value, Optional<MediaType> contextMimeType, Optional<Charset> contextEncoding) {
    Charset existingEncoding = defaultEncoding;
    MediaType mediaType = defaultMediaType;
    if (value instanceof Result) {
      final Optional<MediaType> optionalMediaType = ((Result) value).getMediaType();
      if (optionalMediaType.isPresent()) {
        mediaType = optionalMediaType.get();
        if (mediaType.getCharset().isPresent()) {
          existingEncoding = mediaType.getCharset().orElse(existingEncoding);
        }
      }
    }

    return contextMimeType.orElse(mediaType).withCharset(contextEncoding.orElse(existingEncoding));
  }

  protected class ConnectedInputStreamWrapper extends ProxyInputStream {

    private final ConnectionHandler<?> connectionHandler;

    private ConnectedInputStreamWrapper(InputStream delegate, ConnectionHandler<?> connectionHandler) {
      super(delegate);
      this.connectionHandler = connectionHandler;
    }

    /**
     * Automatically closes the stream if the end of stream was reached.
     *
     * @param n number of bytes read, or -1 if no more bytes are available
     * @throws IOException if the stream could not be closed
     * @since 2.0
     */
    @Override
    protected void afterRead(final int n) throws IOException {
      if (n == EOF) {
        close();
      }
    }

    @Override
    public void close() throws IOException {
      try {
        super.close();
        in = new ClosedInputStream();
      } finally {
        connectionHandler.release();
      }
    }

  }
}
