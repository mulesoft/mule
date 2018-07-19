/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toSet;
import static org.mule.runtime.api.message.Message.of;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaTypeUtils.parseCharset;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.SystemUtils.getDefaultEncoding;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.transaction.TransactionConfig;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.core.internal.util.mediatype.PayloadMediaTypeResolver;
import org.mule.runtime.core.privileged.execution.MessageProcessContext;
import org.mule.runtime.core.privileged.execution.MessageProcessingManager;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.extension.api.runtime.source.SourceCallbackContext;
import org.mule.runtime.module.extension.internal.loader.java.property.MediaTypeModelProperty;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionSourceBinder;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Default implementation of {@link SourceCallback}. Instances are to be created through the {@link #builder()} method.
 *
 * @param <T> the generic type of the output values of the generated results
 * @param <A> the generic type of the attributes of the generated results
 * @since 4.0
 */
class DefaultSourceCallback<T, A> implements SourceCallbackAdapter<T, A> {

  /**
   * A Builder to create instance of {@link DefaultSourceCallback}
   *
   * @param <T> the generic type of the output values of the generated results
   * @param <A> the generic type of the attributes of the generated results
   */
  static class Builder<T, A> {

    private Builder() {}

    private DefaultSourceCallback<T, A> product = new DefaultSourceCallback();

    public Builder<T, A> setSourceModel(SourceModel sourceModel) {
      product.sourceModel = sourceModel;
      product.returnsListOfMessages = returnsListOfMessages(sourceModel);
      product.defaultMediaType = sourceModel.getModelProperty(MediaTypeModelProperty.class)
          .map(MediaTypeModelProperty::getMediaType)
          .orElse(ANY);
      product.notificationModelNames = sourceModel.getNotificationModels()
          .stream()
          .map(NotificationModel::getIdentifier)
          .collect(toSet());

      return this;
    }

    public Builder<T, A> setConfigurationInstance(ConfigurationInstance configurationInstance) {
      product.configurationInstance = configurationInstance;
      return this;
    }

    public Builder<T, A> setTransactionConfig(TransactionConfig transactionConfig) {
      product.transactionConfig = transactionConfig;
      return this;
    }

    public Builder<T, A> setListener(Processor listener) {
      product.listener = listener;
      return this;
    }

    public Builder<T, A> setMuleContext(MuleContext muleContext) {
      product.muleContext = muleContext;
      return this;
    }

    public Builder<T, A> setSource(ExtensionMessageSource messageSource) {
      product.messageSource = messageSource;
      return this;
    }

    public Builder<T, A> setExceptionCallback(ExceptionCallback<ConnectionException> exceptionCallback) {
      product.exceptionCallback = exceptionCallback;
      return this;
    }

    public Builder<T, A> setProcessingManager(MessageProcessingManager processingManager) {
      product.messageProcessingManager = processingManager;
      return this;
    }

    public Builder<T, A> setProcessContextSupplier(Supplier<MessageProcessContext> processContextSupplier) {
      product.processContextSupplier = processContextSupplier;
      return this;
    }

    public Builder<T, A> setCompletionHandlerFactory(SourceCompletionHandlerFactory completionHandlerFactory) {
      product.completionHandlerFactory = completionHandlerFactory;
      return this;
    }

    public Builder<T, A> setCursorStreamProviderFactory(CursorProviderFactory cursorProviderFactory) {
      product.cursorProviderFactory = cursorProviderFactory;
      return this;
    }

    public SourceCallback<T, A> build() {
      checkArgument(product.listener, "listener");
      checkArgument(product.exceptionCallback, "exceptionCallback");
      checkArgument(product.messageProcessingManager, "messageProcessingManager");
      checkArgument(product.processContextSupplier, "processContextSupplier");
      checkArgument(product.completionHandlerFactory, "completionHandlerSupplier");
      checkArgument(product.sourceModel, "source");
      checkArgument(product.cursorProviderFactory, "cursorStreamProviderFactory");
      checkArgument(product.messageSource, "messageSource");
      checkArgument(product.muleContext, "muleContext");

      product.transactionSourceBinder =
          new TransactionSourceBinder(product.messageSource.getExtensionModel(), product.sourceModel, product.muleContext);

      return product;
    }

    private void checkArgument(Object value, String name) {
      Preconditions.checkArgument(value != null, name + " was not set");
    }

  }

  /**
   * @return a new {@link Builder}
   */
  static Builder builder() {
    return new Builder();
  }

  private SourceModel sourceModel;
  private Set<String> notificationModelNames;
  private ConfigurationInstance configurationInstance;
  private Processor listener;
  private MuleContext muleContext;
  private ExtensionMessageSource messageSource;
  private ExceptionCallback<ConnectionException> exceptionCallback;
  private MessageProcessingManager messageProcessingManager;
  private Supplier<MessageProcessContext> processContextSupplier;
  private SourceCompletionHandlerFactory completionHandlerFactory;
  private CursorProviderFactory cursorProviderFactory;
  private TransactionConfig transactionConfig;
  private boolean returnsListOfMessages = false;
  private MediaType defaultMediaType;
  private TransactionSourceBinder transactionSourceBinder;

  private Charset defaultEncoding;

  private Optional<MediaType> mimeTypeInitParam;
  private Optional<Charset> encodingParam;

  private DefaultSourceCallback() {}

  private RunOnce resolveInitializationParams = Once.of(() -> {
    defaultEncoding = getDefaultEncoding(muleContext);

    Map<String, Object> initialisationParameters = messageSource.getInitialisationParameters();

    encodingParam = ofNullable((String) initialisationParameters.get(ENCODING_PARAMETER_NAME))
        .map(encoding -> parseCharset(encoding));
    mimeTypeInitParam = ofNullable((String) initialisationParameters.get(MIME_TYPE_PARAMETER_NAME))
        .map(mimeType -> MediaType.parse(mimeType));
  });

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(Result<T, A> result) {
    handle(result, createContext());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void handle(Result<T, A> result, SourceCallbackContext context) {
    resolveInitializationParams.runOnce();
    checkArgument(context instanceof SourceCallbackContextAdapter, "The supplied context was not created through this callback, "
        + "you naughty developer");

    SourceCallbackContextAdapter contextAdapter = (SourceCallbackContextAdapter) context;
    validateNotifications(contextAdapter);
    MessageProcessContext messageProcessContext = processContextSupplier.get();
    MediaType mediaType = resolveMediaType(result);
    PayloadMediaTypeResolver payloadMediaTypeResolver = new PayloadMediaTypeResolver(getDefaultEncoding(muleContext),
                                                                                     defaultMediaType,
                                                                                     encodingParam,
                                                                                     mimeTypeInitParam);

    SourceResultAdapter resultAdapter =
        new SourceResultAdapter(result, cursorProviderFactory, mediaType, returnsListOfMessages,
                                context.getCorrelationId(), payloadMediaTypeResolver);
    Message message = of(resultAdapter);

    executeFlow(context, messageProcessContext, message);
    contextAdapter.dispatched();
  }

  private void validateNotifications(SourceCallbackContextAdapter contextAdapter) {
    contextAdapter.getNotificationsFunctions().forEach(sourceNotification -> {
      String notificationName = ((ExtensionNotificationFunction) sourceNotification).getActionName();
      checkArgument(notificationModelNames.contains(notificationName),
                    () -> format("Cannot fire notification '%s' since it's not declared by the component.", notificationName));
    });
  }

  private void executeFlow(SourceCallbackContext context, MessageProcessContext messageProcessContext, Message message) {
    SourceCallbackContextAdapter contextAdapter = (SourceCallbackContextAdapter) context;
    messageProcessingManager.processMessage(
                                            new ModuleFlowProcessingTemplate(message, listener,
                                                                             contextAdapter.getNotificationsFunctions(),
                                                                             completionHandlerFactory
                                                                                 .createCompletionHandler(contextAdapter)),
                                            messageProcessContext);
  }

  protected MediaType resolveMediaType(Object value) {
    Charset existingEncoding = defaultEncoding;
    MediaType mediaType = defaultMediaType;
    if (value instanceof Result) {
      final Optional<MediaType> optionalMediaType = ((Result) value).getMediaType();
      if (optionalMediaType.isPresent()) {
        mediaType = optionalMediaType.get();
        existingEncoding = mediaType.getCharset().orElse(existingEncoding);
      }
    }

    return mimeTypeInitParam.orElse(mediaType).withCharset(encodingParam.orElse(existingEncoding));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void onConnectionException(ConnectionException e) {
    exceptionCallback.onException(e);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SourceCallbackContext createContext() {
    return new DefaultSourceCallbackContext(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionSourceBinder getTransactionSourceBinder() {
    return transactionSourceBinder;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConfigurationInstance getConfigurationInstance() {
    return configurationInstance;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public TransactionConfig getTransactionConfig() {
    return transactionConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public SourceConnectionManager getSourceConnectionManager() {
    return messageSource.getSourceConnectionManager();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOwningSourceName() {
    return sourceModel.getName();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getOwningExtensionName() {
    return messageSource.getExtensionModel().getName();
  }
}
