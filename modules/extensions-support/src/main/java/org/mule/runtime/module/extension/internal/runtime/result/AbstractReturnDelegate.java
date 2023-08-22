/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.result;

import static org.apache.commons.io.IOUtils.EOF;
import static org.mule.runtime.api.metadata.MediaType.parseDefinedInApp;
import static org.mule.runtime.api.metadata.MediaTypeUtils.parseCharset;
import static org.mule.runtime.core.api.util.StreamingUtils.supportsStreaming;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.core.internal.util.message.MessageUtils.messageCollection;
import static org.mule.runtime.core.internal.util.message.MessageUtils.messageIterator;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isJavaCollection;
import static org.mule.runtime.extension.api.util.ExtensionMetadataTypeUtils.isMap;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.CONNECTION_PARAM;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.ReturnHandler.nullHandler;
import static org.mule.runtime.module.extension.internal.util.MediaTypeUtils.getDefaultMediaType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getMutableConfigurationStats;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;

import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionHandler;
import org.mule.runtime.api.event.Event;
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
import org.mule.runtime.core.internal.util.collection.TransformingCollection;
import org.mule.runtime.core.internal.util.collection.TransformingIterator;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultCollection;
import org.mule.runtime.core.internal.util.mediatype.MediaTypeDecoratedResultIterator;
import org.mule.runtime.core.internal.util.mediatype.PayloadMediaTypeResolver;
import org.mule.runtime.core.internal.util.message.MessageUtils;
import org.mule.runtime.core.internal.util.message.SdkResultAdapter;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.module.extension.api.runtime.privileged.ExecutionContextAdapter;
import org.mule.runtime.module.extension.internal.runtime.config.MutableConfigurationStats;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.CollectionReturnHandler;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.MapReturnHandler;
import org.mule.runtime.module.extension.internal.runtime.operation.resulthandler.ReturnHandler;
import org.mule.sdk.api.runtime.operation.Result;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

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
public abstract class AbstractReturnDelegate implements ReturnDelegate {

  protected final MuleContext muleContext;
  private boolean returnsListOfMessages = false;
  private final MediaType defaultMediaType;
  private boolean isSpecialHandling = false;
  private ReturnHandler returnHandler = nullHandler();

  private final Charset defaultEncoding;

  /**
   * Creates a new instance
   *
   * @param componentModel the component which produces the return value
   * @param muleContext    the {@link MuleContext} of the owning application
   */
  protected AbstractReturnDelegate(ComponentModel componentModel, MuleContext muleContext) {
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

    defaultEncoding = getDefaultEncoding(muleContext);
    defaultMediaType = getDefaultMediaType(componentModel);
  }

  protected Message toMessage(Object value, ExecutionContextAdapter operationContext) {
    if (value instanceof Event) {
      return ((Event) value).getMessage();
    }

    if (value instanceof org.mule.runtime.extension.api.runtime.operation.Result) {
      value = new SdkResultAdapter<>((org.mule.runtime.extension.api.runtime.operation.Result) value);
    }

    Map<String, Object> params = operationContext.getParameters();
    MediaType contextMimeTypeParam = getContextMimeType(params);
    Charset contextEncodingParam = getContextEncoding(params);
    final MediaType mediaType = resolveMediaType(value, contextMimeTypeParam, contextEncodingParam);
    final CoreEvent event = operationContext.getEvent();
    final CursorProviderFactory cursorProviderFactory = operationContext.getCursorProviderFactory();

    ComponentLocation originatingLocation = operationContext.getComponent().getLocation();

    if (value instanceof Result) {
      Result resultValue = (Result) value;
      if (resultValue.getOutput() instanceof InputStream) {
        ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
        if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
          resultValue = resultValue.copy()
              .output(new ConnectedInputStreamWrapper((InputStream) resultValue.getOutput(), connectionHandler,
                                                      getDecrementActiveComponentTask(operationContext)))
              .build();
        }
      }
      return isSpecialHandling && returnHandler.handles(resultValue.getOutput())
          ? MessageUtils.toMessage(resultValue, mediaType, cursorProviderFactory, event, returnHandler.getDataType(),
                                   originatingLocation)
          : MessageUtils.toMessage(resultValue, mediaType, cursorProviderFactory, event, originatingLocation);
    } else {
      PayloadMediaTypeResolver payloadMediaTypeResolver = new PayloadMediaTypeResolver(defaultEncoding,
                                                                                       defaultMediaType,
                                                                                       contextEncodingParam,
                                                                                       contextMimeTypeParam);
      if (value instanceof Collection && returnsListOfMessages) {
        value = toLazyMessageCollection((Collection) value, operationContext, event);
        value = messageCollection(new MediaTypeDecoratedResultCollection((Collection) value, payloadMediaTypeResolver),
                                  cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                                  originatingLocation);
      } else if (value instanceof Iterator) {
        if (returnsListOfMessages) {
          Iterator<Result> iterator = TransformingIterator.from((Iterator<Object>) value, toResult(operationContext, event));
          value = messageIterator(new MediaTypeDecoratedResultIterator(iterator, payloadMediaTypeResolver),
                                  cursorProviderFactory, ((BaseEventContext) event.getContext()).getRootContext(),
                                  originatingLocation);
        }
      }

      value = streamingContent(value, operationContext, cursorProviderFactory,
                               ((BaseEventContext) event.getContext()).getRootContext(), originatingLocation);

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


  private Collection<Result> toLazyMessageCollection(Collection<?> values,
                                                     ExecutionContextAdapter operationContext,
                                                     CoreEvent event) {
    return new TransformingCollection<>((Collection<Object>) values, toResult(operationContext, event));
  }

  private Function<Object, Result> toResult(ExecutionContextAdapter operationContext, CoreEvent event) {
    Function<Object, Result> transformer = value -> {
      Result result;
      if (value instanceof Result) {
        result = (Result) value;
      } else if (value instanceof org.mule.runtime.extension.api.runtime.operation.Result) {
        result = new SdkResultAdapter<>((org.mule.runtime.extension.api.runtime.operation.Result) value);
      } else {
        throw new IllegalArgumentException("Result was expected but value found instead: " + value.getClass().getName());
      }

      if (result.getOutput() instanceof InputStream) {
        ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
        if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
          result = result.copy().output(StreamingUtils
              .streamingContent(new ConnectedInputStreamWrapper((InputStream) result.getOutput(),
                                                                connectionHandler,
                                                                getDecrementActiveComponentTask(operationContext)),
                                operationContext.getCursorProviderFactory(), event,
                                operationContext.getComponent().getLocation()))
              .build();
        }
      }
      return result;
    };
    return transformer;
  }

  private MediaType getContextMimeType(Map<String, Object> params) {
    String mimeType = (String) params.get(MIME_TYPE_PARAMETER_NAME);
    return mimeType != null ? parseDefinedInApp(mimeType) : null;
  }

  private Charset getContextEncoding(Map<String, Object> params) {
    String encoding = (String) params.get(ENCODING_PARAMETER_NAME);
    return encoding != null ? parseCharset(encoding) : null;
  }

  private Object streamingContent(Object value,
                                  ExecutionContextAdapter operationContext,
                                  CursorProviderFactory cursorProviderFactory,
                                  BaseEventContext eventContext,
                                  ComponentLocation originatingLocation) {
    if (value instanceof InputStream) {
      ConnectionHandler connectionHandler = (ConnectionHandler) operationContext.getVariable(CONNECTION_PARAM);
      if (connectionHandler != null && supportsStreaming(operationContext.getComponentModel())) {
        value = new ConnectedInputStreamWrapper((InputStream) value, connectionHandler,
                                                getDecrementActiveComponentTask(operationContext));
      }
    }

    return StreamingUtils.streamingContent(value, cursorProviderFactory, eventContext, originatingLocation);
  }

  /**
   * If provided, mimeType and encoding configured as operation parameters will take precedence over what comes with the message's
   * {@link DataType}.
   *
   * @param value           the operation's value
   * @param contextMimeType the mimeType specified in the operation
   * @param contextEncoding the encoding specified in the operation
   * @return the resolved {@link MediaType}
   */
  protected MediaType resolveMediaType(Object value, MediaType contextMimeType, Charset contextEncoding) {
    if (contextEncoding == null) {
      contextEncoding = defaultEncoding;
    }
    if (contextMimeType == null) {
      MediaType mediaType = defaultMediaType;
      if (value instanceof Result) {
        final Optional<MediaType> optionalMediaType = ((Result) value).getMediaType();
        if (optionalMediaType.isPresent()) {
          mediaType = optionalMediaType.get();
          if (mediaType.getCharset().isPresent()) {
            contextEncoding = mediaType.getCharset().orElse(contextEncoding);
          }
        }
      }

      contextMimeType = mediaType;
    }

    return contextMimeType.withCharset(contextEncoding);
  }

  private Runnable getDecrementActiveComponentTask(ExecutionContextAdapter executionContext) {
    MutableConfigurationStats mutableStats = getMutableConfigurationStats(executionContext);
    return mutableStats != null ? mutableStats::discountActiveComponent : null;
  }

  protected class ConnectedInputStreamWrapper extends ProxyInputStream {

    private final ConnectionHandler<?> connectionHandler;
    private final Runnable onClose;
    private AtomicBoolean alreadyClosed = new AtomicBoolean(false);

    private ConnectedInputStreamWrapper(InputStream delegate, ConnectionHandler<?> connectionHandler, Runnable onClose) {
      super(delegate);
      this.connectionHandler = connectionHandler;
      this.onClose = onClose;
    }

    /**
     * Automatically closes the stream if the end of stream was reached.
     *
     * @param n number of bytes read, or -1 if no more bytes are available
     * @throws IOException if the stream could not be closed
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
        try {
          connectionHandler.release();
        } finally {
          if (onClose != null && alreadyClosed.compareAndSet(false, true)) {
            onClose.run();
          }
        }
      }
    }

  }
}
