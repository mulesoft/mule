/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.operation;

import static org.mule.metadata.api.model.MetadataFormat.JAVA;
import static org.mule.runtime.api.metadata.MediaType.ANY;
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
import org.mule.metadata.api.model.ObjectType;
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
import org.mule.runtime.core.internal.util.message.MessageUtils;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.CollectionReturnHandler;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.MapReturnHandler;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.ReturnHandler;

import java.io.IOException;
import java.io.InputStream;
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
    defaultMediaType = getDefaultMediaType(componentModel);
  }

  protected Message toMessage(Object value, ExecutionContextAdapter operationContext) {
    final MediaType mediaType = resolveMediaType(value, operationContext);
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
      if (value instanceof Collection && returnsListOfMessages) {
        value = toMessageCollection((Collection<Result>) value, cursorProviderFactory, event);
      } else if (value instanceof Iterator && returnsListOfMessages) {
        value = toMessageIterator((Iterator<Result>) value, cursorProviderFactory, event);
      }

      value = streamingContent(value, operationContext, cursorProviderFactory, event);

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

  private Object streamingContent(Object value,
                                  ExecutionContextAdapter operationContext,
                                  CursorProviderFactory cursorProviderFactory,
                                  CoreEvent event) {
    if (value instanceof InputStream) {
      ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
      if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
        value = new ConnectedInputStreamWrapper((InputStream) value, connectionHandler);
      }
    }

    return StreamingUtils.streamingContent(value, cursorProviderFactory, event);
  }

  /**
   * If provided, mimeType and encoding configured as operation parameters will take precedence over what comes with the message's
   * {@link DataType}.
   *
   * @param value
   * @param operationContext
   * @return
   */
  protected MediaType resolveMediaType(Object value, ExecutionContextAdapter<ComponentModel> operationContext) {
    Charset existingEncoding = getDefaultEncoding(muleContext);
    MediaType mediaType = defaultMediaType;
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
      mediaType = mediaType.withCharset(parseCharset(operationContext.getParameter(ENCODING_PARAMETER_NAME)));
    } else {
      mediaType = mediaType.withCharset(existingEncoding);
    }

    return mediaType;
  }

  private class ConnectedInputStreamWrapper extends InputStream {

    private final InputStream delegate;
    private final ConnectionHandler<?> connectionHandler;

    private ConnectedInputStreamWrapper(InputStream delegate, ConnectionHandler<?> connectionHandler) {
      this.delegate = delegate;
      this.connectionHandler = connectionHandler;
    }

    @Override
    public int read() throws IOException {
      return delegate.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
      return delegate.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      return delegate.read(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
      return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
      return delegate.available();
    }

    @Override
    public void close() throws IOException {
      try {
        delegate.close();
      } finally {
        connectionHandler.release();
      }
    }

    @Override
    public void mark(int readlimit) {
      delegate.mark(readlimit);
    }

    @Override
    public void reset() throws IOException {
      delegate.reset();
    }

    @Override
    public boolean markSupported() {
      return delegate.markSupported();
    }
  }
}
