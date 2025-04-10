/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static org.mule.runtime.api.metadata.MediaType.parseDefinedInApp;
import static org.mule.runtime.api.metadata.MediaTypeUtils.parseCharset;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.ENCODING_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.ExtensionProperties.MIME_TYPE_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.runtime.source.poll.PollingSourceWrapper.ACCEPTED_POLL_ITEM_INFORMATION;
import static org.mule.runtime.module.extension.internal.util.MediaTypeUtils.getDefaultMediaType;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.returnsListOfMessages;

import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.meta.model.notification.NotificationModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.util.Preconditions;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.util.func.Once;
import org.mule.runtime.core.api.util.func.Once.RunOnce;
import org.mule.runtime.core.internal.execution.ExceptionCallback;
import org.mule.runtime.core.internal.execution.MessageProcessContext;
import org.mule.runtime.core.internal.execution.MessageProcessingManager;
import org.mule.runtime.core.internal.execution.PollItemInformation;
import org.mule.runtime.core.internal.execution.SourceResultAdapter;
import org.mule.runtime.core.internal.util.mediatype.PayloadMediaTypeResolver;
import org.mule.runtime.core.privileged.transaction.TransactionConfig;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.SourceCallback;
import org.mule.runtime.module.extension.internal.runtime.source.trace.SourceDistributedTraceContextManager;
import org.mule.runtime.module.extension.internal.runtime.transaction.TransactionSourceBinder;
import org.mule.sdk.api.runtime.operation.Result;
import org.mule.sdk.api.runtime.source.DistributedTraceContextManager;
import org.mule.sdk.api.runtime.source.SourceCallbackContext;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Default implementation of {@link SourceCallback}. Instances are to be created through the {@link #builder(ProfilingService)}
 * method.
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

    private final DefaultSourceCallback<T, A> product;

    private Builder(ProfilingService profilingService) {
      product = new DefaultSourceCallback<>(profilingService);
    }

    public Builder<T, A> setSourceModel(SourceModel sourceModel) {
      product.sourceModel = sourceModel;
      product.returnsListOfMessages = returnsListOfMessages(sourceModel);
      product.defaultMediaType = getDefaultMediaType(sourceModel);
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

    public Builder<T, A> setDefaultEncoding(Charset defaultEncoding) {
      product.defaultEncoding = defaultEncoding;

      return this;
    }

    public Builder<T, A> setApplicationName(String applicationName) {
      product.applicationName = applicationName;
      return this;
    }

    public Builder<T, A> setNotificationDispatcher(NotificationDispatcher notificationDispatcher) {
      product.notificationDispatcher = notificationDispatcher;
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

    public Builder<T, A> setProcessContext(MessageProcessContext processContext) {
      product.messageProcessContext = processContext;
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

    public Builder<T, A> setErrorAfterTimeout(boolean errorAfterTimeout) {
      product.errorAfterTimeout = errorAfterTimeout;
      return this;
    }

    public org.mule.sdk.api.runtime.source.SourceCallback<T, A> build() {
      checkArgument(product.listener, "listener");
      checkArgument(product.exceptionCallback, "exceptionCallback");
      checkArgument(product.messageProcessingManager, "messageProcessingManager");
      checkArgument(product.messageProcessContext, "processContext");
      checkArgument(product.completionHandlerFactory, "completionHandlerSupplier");
      checkArgument(product.sourceModel, "source");
      checkArgument(product.cursorProviderFactory, "cursorStreamProviderFactory");
      checkArgument(product.messageSource, "messageSource");
      checkArgument(product.applicationName, "applicationName");
      checkArgument(product.notificationDispatcher, "notificationDispatcher");

      product.transactionSourceBinder =
          new TransactionSourceBinder(product.messageSource.getExtensionModel(), product.sourceModel, product.applicationName,
                                      product.notificationDispatcher);

      return product;
    }

    private void checkArgument(Object value, String name) {
      Preconditions.checkArgument(value != null, name + " was not set");
    }

  }

  /**
   * @return a new {@link Builder}
   */
  static Builder builder(ProfilingService profilingService) {
    return new Builder(profilingService);
  }

  private SourceModel sourceModel;
  private Set<String> notificationModelNames;
  private ConfigurationInstance configurationInstance;
  private Processor listener;
  private Charset defaultEncoding;

  private String applicationName;
  private NotificationDispatcher notificationDispatcher;
  private ExtensionMessageSource messageSource;
  private ExceptionCallback<ConnectionException> exceptionCallback;
  private MessageProcessingManager messageProcessingManager;
  private MessageProcessContext messageProcessContext;
  private SourceCompletionHandlerFactory completionHandlerFactory;
  private CursorProviderFactory cursorProviderFactory;
  private TransactionConfig transactionConfig;
  private boolean returnsListOfMessages = false;
  private MediaType defaultMediaType;
  private TransactionSourceBinder transactionSourceBinder;
  private final ProfilingService profilingService;

  private MediaType mimeTypeInitParam;
  private Charset encodingParam;
  private boolean errorAfterTimeout;

  private DefaultSourceCallback(ProfilingService profilingService) {
    this.profilingService = profilingService;
  }

  private final RunOnce resolveInitializationParams = Once.of(() -> {
    Map<String, Object> initialisationParameters = messageSource.getInitialisationParameters();

    String encoding = (String) initialisationParameters.get(ENCODING_PARAMETER_NAME);
    if (encoding != null) {
      encodingParam = parseCharset(encoding);
    } else {
      encodingParam = defaultEncoding;
    }

    String mimeType = (String) initialisationParameters.get(MIME_TYPE_PARAMETER_NAME);
    if (mimeType != null) {
      mimeTypeInitParam = parseDefinedInApp(mimeType);
    }
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
    MediaType mediaType = resolveMediaType(result);
    PayloadMediaTypeResolver payloadMediaTypeResolver = new PayloadMediaTypeResolver(defaultEncoding,
                                                                                     defaultMediaType,
                                                                                     encodingParam,
                                                                                     mimeTypeInitParam);

    String name = null;
    Map<String, String> attributes = new HashMap<>();

    DistributedTraceContextManager distributedTraceContextManager = context.getDistributedSourceTraceContext();

    if (distributedTraceContextManager instanceof SourceDistributedTraceContextManager distributedSourceTraceContextManager) {
      name = distributedSourceTraceContextManager.getSpanName();
      attributes = distributedSourceTraceContextManager.getSpanRootAttributes();
    }
    SourceResultAdapter resultAdapter =
        new SourceResultAdapter(result, cursorProviderFactory, mediaType, returnsListOfMessages,
                                context.getCorrelationId(), payloadMediaTypeResolver, context.getDistributedSourceTraceContext(),
                                name,
                                attributes,
                                context.getVariable(ACCEPTED_POLL_ITEM_INFORMATION).map(PollItemInformation.class::cast));

    executeFlow(context, messageProcessContext, resultAdapter, distributedTraceContextManager);
    contextAdapter.dispatched();
  }

  private void validateNotifications(SourceCallbackContextAdapter contextAdapter) {
    contextAdapter.getNotificationsFunctions().forEach(sourceNotification -> {
      String notificationName = ((ExtensionNotificationFunction) sourceNotification).getActionName();
      checkArgument(notificationModelNames.contains(notificationName),
                    () -> format("Cannot fire notification '%s' since it's not declared by the component.", notificationName));
    });
  }

  private void executeFlow(SourceCallbackContext context, MessageProcessContext messageProcessContext,
                           SourceResultAdapter resultAdapter,
                           DistributedTraceContextManager distributedSourceTraceContextManager) {
    SourceCallbackContextAdapter contextAdapter = (SourceCallbackContextAdapter) context;
    messageProcessingManager.processMessage(
                                            new ExtensionsFlowProcessingTemplate(resultAdapter, listener,
                                                                                 contextAdapter.getNotificationsFunctions(),
                                                                                 completionHandlerFactory
                                                                                     .createCompletionHandler(contextAdapter)),
                                            messageProcessContext,
                                            distributedSourceTraceContextManager);
  }

  protected MediaType resolveMediaType(Object value) {
    Charset existingEncoding = encodingParam;
    MediaType mediaType = mimeTypeInitParam;
    if (mediaType == null) {
      if (value instanceof Result result) {
        final Optional<MediaType> optionalMediaType = result.getMediaType();
        if (optionalMediaType.isPresent()) {
          mediaType = optionalMediaType.get();
          existingEncoding = mediaType.getCharset().orElse(existingEncoding);
        }
      }

      if (mediaType == null) {
        mediaType = defaultMediaType;
      }
    }
    return mediaType.withCharset(existingEncoding);
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
    return new DefaultSourceCallbackContext(this, profilingService, errorAfterTimeout);
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

  /**
   * {@inheritDoc}
   */
  @Override
  public ComponentLocation getSourceLocation() {
    return messageSource.getLocation();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getTimeout() {
    return getTransactionConfig().getTimeout();
  }
}
