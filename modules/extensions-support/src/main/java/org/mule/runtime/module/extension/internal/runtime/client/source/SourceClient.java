/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.client.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import static org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSetUtils.getResolverSetFromComponentParameterization;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toBackPressureStrategy;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.api.parameterization.ComponentParameterization;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.SingleResourceTransactionFactoryManager;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.extension.api.client.source.SourceResultCallback;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapterFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.function.Consumer;

import org.slf4j.Logger;

public class SourceClient implements Lifecycle {

  private static final Logger LOGGER = getLogger(SourceClient.class);

  private final ExtensionMessageSource source;
  private final MuleContext muleContext;

  public static <T, A> SourceClient from(ExtensionModel extensionModel,
                                  SourceModel sourceModel,
                                  DefaultSourceParameterizer parameterizer,
                                  Consumer<SourceResultCallback<T, A>> callback,
                                  ExtensionManager extensionManager,
                                  ExpressionManager expressionManager,
                                  StreamingManager streamingManager,
                                  ReflectionCache reflectionCache,
                                  NotificationDispatcher notificationDispatcher,
                                  SingleResourceTransactionFactoryManager transactionFactoryManager,
                                  MuleContext muleContext) {

    final BackPressureStrategy backPressureStrategy = toBackPressureStrategy(parameterizer.getBackPressureMode());
    final CursorProviderFactory cursorProviderFactory = parameterizer.getCursorProviderFactory(streamingManager);
    final SourceAdapterFactory sourceAdapterFactory = newSourceAdapterFactory(extensionModel,
                                                                              sourceModel,
                                                                              parameterizer,
                                                                              cursorProviderFactory,
                                                                              backPressureStrategy,
                                                                              reflectionCache,
                                                                              expressionManager,
                                                                              muleContext);

    ExtensionMessageSource source = new ExtensionMessageSource(extensionModel,
                                                               sourceModel,
                                                               sourceAdapterFactory,
                                                               getConfigurationProvider(extensionManager, parameterizer),
                                                               true,
                                                               parameterizer.getRetryPolicyTemplate(),
                                                               cursorProviderFactory,
                                                               backPressureStrategy,
                                                               extensionManager,
                                                               notificationDispatcher,
                                                               transactionFactoryManager,
                                                               "");

    return new SourceClient(source, muleContext);
  }

  private SourceClient(ExtensionMessageSource source, MuleContext muleContext) {
    this.source = source;
    this.muleContext = muleContext;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(source, true, muleContext);
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

  private static SourceAdapterFactory newSourceAdapterFactory(ExtensionModel extensionModel,
                                                              SourceModel sourceModel,
                                                              DefaultSourceParameterizer parameterizer,
                                                              CursorProviderFactory cursorProviderFactory,
                                                              BackPressureStrategy backPressureStrategy,
                                                              ReflectionCache reflectionCache,
                                                              ExpressionManager expressionManager,
                                                              MuleContext muleContext) {

    ComponentParameterization.Builder<SourceModel> paramsBuilder = ComponentParameterization.builder(sourceModel);
    parameterizer.setValuesOn(paramsBuilder);

      ResolverSet resolverSet;
    try {
      resolverSet = getResolverSetFromComponentParameterization(
        paramsBuilder.build(),
        muleContext,
        true,
        reflectionCache,
        expressionManager,
        "");
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(e.getMessage()), e);
    }


    return new SourceAdapterFactory(extensionModel,
                                    sourceModel,
                                    resolverSet,
                                    resolverSet,
                                    resolverSet,
                                    cursorProviderFactory,
                                    backPressureStrategy,
                                    expressionManager,
                                    muleContext);
  }

  private static ConfigurationProvider getConfigurationProvider(ExtensionManager extensionManager, DefaultSourceParameterizer parameterizer) {
    return extensionManager.getConfigurationProvider(parameterizer.getConfigRef())
      .orElseThrow(() -> new IllegalArgumentException("No configuration registered for key '" + parameterizer.getConfigRef() + "'"));
  }
}
