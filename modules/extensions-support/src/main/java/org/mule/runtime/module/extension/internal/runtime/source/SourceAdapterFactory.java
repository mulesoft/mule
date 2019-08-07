/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import static java.lang.String.format;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getSourceFactory;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toBackPressureAction;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.util.MessagingExceptionResolver;
import org.mule.runtime.extension.api.runtime.config.ConfigurationInstance;
import org.mule.runtime.extension.api.runtime.source.BackPressureAction;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.Optional;

/**
 * A factory for {@link SourceAdapter} instances
 */
public class SourceAdapterFactory {

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;
  private final ResolverSet sourceParameters;
  private final ResolverSet successCallbackParameters;
  private final ResolverSet errorCallbackParameters;
  private final CursorProviderFactory cursorProviderFactory;
  private final Optional<BackPressureAction> backPressureAction;
  private final ReflectionCache reflectionCache;
  private final ExpressionManager expressionManager;
  private ConfigurationProperties properties;
  private final MuleContext muleContext;

  public SourceAdapterFactory(ExtensionModel extensionModel,
                              SourceModel sourceModel,
                              ResolverSet sourceParameters,
                              ResolverSet successCallbackParameters,
                              ResolverSet errorCallbackParameters,
                              CursorProviderFactory cursorProviderFactory,
                              BackPressureStrategy backPressureStrategy,
                              ReflectionCache reflectionCache,
                              ExpressionManager expressionManager,
                              ConfigurationProperties properties,
                              MuleContext muleContext) {
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
    this.sourceParameters = sourceParameters;
    this.successCallbackParameters = successCallbackParameters;
    this.errorCallbackParameters = errorCallbackParameters;
    this.cursorProviderFactory = cursorProviderFactory;
    this.backPressureAction = toBackPressureAction(backPressureStrategy);
    this.reflectionCache = reflectionCache;
    this.expressionManager = expressionManager;
    this.properties = properties;
    this.muleContext = muleContext;
  }

  /**
   * Creates a new {@link SourceAdapter}
   *
   * @param configurationInstance an {@link Optional} {@link ConfigurationInstance} in case the source requires a config
   * @param sourceCallbackFactory a {@link SourceCallbackFactory}
   * @return a new {@link SourceAdapter}
   */
  public SourceAdapter createAdapter(Optional<ConfigurationInstance> configurationInstance,
                                     SourceCallbackFactory sourceCallbackFactory,
                                     Component component,
                                     SourceConnectionManager connectionManager,
                                     MessagingExceptionResolver exceptionResolver) {
    return createAdapter(configurationInstance, sourceCallbackFactory, component, connectionManager, exceptionResolver, false);
  }

  /**
   * Creates a new {@link SourceAdapter}
   *
   * @param configurationInstance an {@link Optional} {@link ConfigurationInstance} in case the source requires a config
   * @param sourceCallbackFactory a {@link SourceCallbackFactory}
   *
   * @param restarting            indicates if the creation of the adapter was triggered after by a restart
   *
   * @return a new {@link SourceAdapter}
   */
  public SourceAdapter createAdapter(Optional<ConfigurationInstance> configurationInstance,
                                     SourceCallbackFactory sourceCallbackFactory,
                                     Component component,
                                     SourceConnectionManager connectionManager,
                                     MessagingExceptionResolver exceptionResolver,
                                     boolean restarting) {
    Source source = getSourceFactory(sourceModel).createSource();
    try {
      SourceConfigurer sourceConfigurer = new SourceConfigurer(sourceModel, component.getLocation(), sourceParameters,
                                                               expressionManager, properties, muleContext, restarting);
      source = sourceConfigurer.configure(source, configurationInstance);
      return new SourceAdapter(extensionModel,
                               sourceModel,
                               source,
                               configurationInstance,
                               cursorProviderFactory,
                               sourceCallbackFactory,
                               component,
                               connectionManager,
                               sourceParameters,
                               successCallbackParameters,
                               errorCallbackParameters,
                               exceptionResolver,
                               backPressureAction);
    } catch (Exception e) {
      throw new MuleRuntimeException(createStaticMessage(format("Could not create generator for source '%s'",
                                                                sourceModel.getName())),
                                     e);
    }
  }

  public ResolverSet getSourceParameters() {
    return sourceParameters;
  }
}
