/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.api.component.AbstractComponent.LOCATION_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toBackPressureStrategy;

import static java.util.Collections.emptySet;
import static java.util.Optional.empty;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.config.ArtifactEncoding;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.parameter.ParameterizedModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.api.util.collection.SmallMap;
import org.mule.runtime.core.api.Injector;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.internal.exception.MessagingExceptionResolver;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.core.privileged.exception.MessagingException;
import org.mule.runtime.dsl.api.component.config.DefaultComponentLocation;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.client.source.SourceParameterizer;
import org.mule.runtime.extension.api.client.source.SourceResultHandler;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.client.params.BaseParameterizer;
import org.mule.runtime.module.extension.internal.runtime.resolver.NullResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapterFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Optional;
import java.util.function.Consumer;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * {@link ExtensionsClient} delegate class for creating and operating message sources
 *
 * @since 4.5.0
 */
public class SourceClient<T, A> implements Lifecycle {

  private static final Logger LOGGER = getLogger(SourceClient.class);

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final Consumer<SourceParameterizer> sourceParameterizerConsumer;
  private final Consumer<SourceResultHandler<T, A>> handlerConsumer;
  private final ExtensionManager extensionManager;
  private final StreamingManager streamingManager;
  private final ErrorTypeLocator errorTypeLocator;
  private final ReflectionCache reflectionCache;
  private final ExtendedExpressionManager expressionManager;
  private final NotificationDispatcher notificationDispatcher;
  private final MuleContext muleContext;
  private final Injector injector;
  private final ClassLoader extensionClassloader;

  private ExtensionMessageSource source;
  private MessagingExceptionResolver messagingExceptionResolver;
  private Optional<ConfigurationProvider> configurationProvider = empty();

  @Inject
  private ArtifactEncoding artifactEncoding;

  public SourceClient(ExtensionModel extensionModel,
                      SourceModel sourceModel,
                      Consumer<SourceParameterizer> sourceParameterizerConsumer,
                      Consumer<SourceResultHandler<T, A>> handlerConsumer,
                      ExtensionManager extensionManager,
                      StreamingManager streamingManager,
                      ErrorTypeLocator errorTypeLocator,
                      ReflectionCache reflectionCache,
                      ExtendedExpressionManager expressionManager,
                      NotificationDispatcher notificationDispatcher,
                      MuleContext muleContext,
                      Injector injector) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.sourceParameterizerConsumer = sourceParameterizerConsumer;
    this.handlerConsumer = handlerConsumer;
    this.extensionManager = extensionManager;
    this.streamingManager = streamingManager;
    this.errorTypeLocator = errorTypeLocator;
    this.reflectionCache = reflectionCache;
    this.expressionManager = expressionManager;
    this.notificationDispatcher = notificationDispatcher;
    this.muleContext = muleContext;
    this.injector = injector;

    extensionClassloader = getClassLoader(extensionModel);
  }

  @Override
  public void initialise() throws InitialisationException {
    DefaultSourceParameterizer parameterizer = new DefaultSourceParameterizer();
    sourceParameterizerConsumer.accept(parameterizer);

    final BackPressureStrategy backPressureStrategy = toBackPressureStrategy(parameterizer.getBackPressureMode());
    final CursorProviderFactory cursorProviderFactory = parameterizer.getCursorProviderFactory(streamingManager);
    final SourceAdapterFactory sourceAdapterFactory = newSourceAdapterFactory(parameterizer,
                                                                              cursorProviderFactory,
                                                                              backPressureStrategy);

    configurationProvider = resolveConfigurationProvider(extensionManager, parameterizer);
    source = new ExtensionMessageSource(extensionModel,
                                        sourceModel,
                                        sourceAdapterFactory,
                                        configurationProvider.orElse(null),
                                        true,
                                        parameterizer.getRetryPolicyTemplate(),
                                        cursorProviderFactory,
                                        backPressureStrategy,
                                        extensionManager,
                                        notificationDispatcher,
                                        "");

    source.setAnnotations(SmallMap.of(LOCATION_KEY, DefaultComponentLocation.from(sourceModel.getName())));
    source.setListener(event -> event);
    initialiseIfNeeded(source, injector);
    source.setMessageProcessingManager(new ExtensionsClientMessageProcessingManager(this, handlerConsumer));
    messagingExceptionResolver = new MessagingExceptionResolver(source);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(source);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(source);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(source, LOGGER);
  }

  ResolverSet toResolverSet(BaseParameterizer parameterizer, ParameterizedModel model) {
    ComponentParameterization.Builder paramsBuilder = ComponentParameterization.builder(model);
    parameterizer.setValuesOn(paramsBuilder);

    try {
      ResolverSet resolverSet = getResolverSetFromComponentParameterization(
                                                                            paramsBuilder.build(),
                                                                            muleContext,
                                                                            true,
                                                                            reflectionCache,
                                                                            expressionManager,
                                                                            injector,
                                                                            "", artifactEncoding);

      resolverSet.initialise();
      return resolverSet;
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage("Exception creating ResolverSet: " + e.getMessage()), e);
    }
  }

  Optional<ConfigurationInstance> resolveConfigurationInstance(CoreEvent event) {
    return configurationProvider.map(provider -> provider.get(event));
  }

  ClassLoader getExtensionClassLoader() {
    return extensionClassloader;
  }

  SourceModel getSourceModel() {
    return sourceModel;
  }

  MessagingException asMessagingException(Throwable t, CoreEvent event) {
    MessagingException exception = t instanceof MessagingException
        ? (MessagingException) t
        : new MessagingException(createStaticMessage(t.getMessage()), event, t, source);
    return messagingExceptionResolver.resolve(exception, errorTypeLocator, emptySet());
  }

  private SourceAdapterFactory newSourceAdapterFactory(DefaultSourceParameterizer parameterizer,
                                                       CursorProviderFactory cursorProviderFactory,
                                                       BackPressureStrategy backPressureStrategy) {
    return new SourceAdapterFactory(extensionModel,
                                    sourceModel,
                                    toResolverSet(parameterizer, sourceModel),
                                    NullResolverSet.INSTANCE,
                                    NullResolverSet.INSTANCE,
                                    cursorProviderFactory,
                                    backPressureStrategy,
                                    expressionManager,
                                    muleContext);
  }

  private Optional<ConfigurationProvider> resolveConfigurationProvider(ExtensionManager extensionManager,
                                                                       DefaultSourceParameterizer parameterizer) {
    if (isBlank(parameterizer.getConfigRef())) {
      return empty();
    }

    Optional<ConfigurationProvider> cp = extensionManager.getConfigurationProvider(parameterizer.getConfigRef());
    if (!cp.isPresent()) {
      throw new IllegalArgumentException("No configuration registered for key '" + parameterizer.getConfigRef() + "'");
    }

    return cp;
  }
}
