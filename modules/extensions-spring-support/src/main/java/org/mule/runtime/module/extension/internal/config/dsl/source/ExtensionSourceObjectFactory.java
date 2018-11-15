/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.source;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toBackPressureStrategy;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.loader.java.property.BackPressureStrategyModelProperty;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapterFactory;
import org.mule.runtime.module.extension.internal.util.ReflectionCache;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.common.base.Joiner;


/**
 * An {@link AbstractExtensionObjectFactory} that produces instances of {@link ExtensionMessageSource}
 *
 * @since 4.0
 */
public class ExtensionSourceObjectFactory extends AbstractExtensionObjectFactory<ExtensionMessageSource> {

  @Inject
  private ReflectionCache reflectionCache;

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;

  private ConfigurationProvider configurationProvider;
  private RetryPolicyTemplate retryPolicyTemplate;
  private CursorProviderFactory cursorProviderFactory;
  private Boolean primaryNodeOnly = null;
  private BackPressureStrategy backPressureStrategy = null;

  public ExtensionSourceObjectFactory(ExtensionModel extensionModel, SourceModel sourceModel, MuleContext muleContext) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
  }

  @Override
  public ExtensionMessageSource doGetObject() {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      getParametersResolver().checkParameterGroupExclusiveness(Optional.of(sourceModel),
                                                               sourceModel.getParameterGroupModels(),
                                                               parameters.keySet());
      ResolverSet nonCallbackParameters = getNonCallbackParameters();

      if (nonCallbackParameters.isDynamic()) {
        throw dynamicParameterException(nonCallbackParameters, sourceModel);
      }

      ResolverSet responseCallbackParameters = getCallbackParameters(sourceModel.getSuccessCallback());
      ResolverSet errorCallbackParameters = getCallbackParameters(sourceModel.getErrorCallback());

      initialiseIfNeeded(nonCallbackParameters, true, muleContext);
      initialiseIfNeeded(responseCallbackParameters, true, muleContext);
      initialiseIfNeeded(errorCallbackParameters, true, muleContext);

      final BackPressureStrategy backPressureStrategy = getBackPressureStrategy();

      return new ExtensionMessageSource(extensionModel,
                                        sourceModel,
                                        getSourceAdapterFactory(nonCallbackParameters,
                                                                responseCallbackParameters,
                                                                errorCallbackParameters,
                                                                backPressureStrategy),
                                        configurationProvider,
                                        primaryNodeOnly != null ? primaryNodeOnly : sourceModel.runsOnPrimaryNodeOnly(),
                                        getRetryPolicyTemplate(),
                                        cursorProviderFactory,
                                        backPressureStrategy,
                                        muleContext.getExtensionManager());
    });
  }

  private BackPressureStrategy getBackPressureStrategy() {
    if (backPressureStrategy != null) {
      return backPressureStrategy;
    }

    return sourceModel.getModelProperty(BackPressureStrategyModelProperty.class)
        .map(p -> toBackPressureStrategy(p.getDefaultMode()))
        .orElse(WAIT);
  }

  private ResolverSet getNonCallbackParameters() throws ConfigurationException {
    return getParametersResolver().getParametersAsResolverSet(muleContext, sourceModel, sourceModel.getParameterGroupModels());
  }

  private ResolverSet getCallbackParameters(Optional<SourceCallbackModel> callbackModel) throws ConfigurationException {
    if (callbackModel.isPresent()) {
      return getParametersResolver().getParametersAsResolverSet(callbackModel.get(), muleContext);
    }

    return new ResolverSet(muleContext);
  }

  private SourceAdapterFactory getSourceAdapterFactory(ResolverSet nonCallbackParameters,
                                                       ResolverSet successCallbackParameters,
                                                       ResolverSet errorCallbackParameters,
                                                       BackPressureStrategy backPressureStrategy) {
    return new SourceAdapterFactory(extensionModel,
                                    sourceModel,
                                    nonCallbackParameters,
                                    successCallbackParameters,
                                    errorCallbackParameters,
                                    cursorProviderFactory,
                                    backPressureStrategy,
                                    reflectionCache,
                                    expressionManager,
                                    properties,
                                    muleContext);
  }

  private RetryPolicyTemplate getRetryPolicyTemplate() throws ConfigurationException {
    return retryPolicyTemplate;
  }

  public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  private ConfigurationException dynamicParameterException(ResolverSet resolverSet, SourceModel model) {
    List<String> dynamicParams = resolverSet.getResolvers().entrySet().stream().filter(entry -> entry.getValue().isDynamic())
        .map(entry -> entry.getKey()).collect(toList());

    return new ConfigurationException(
                                      createStaticMessage(format("The '%s' message source is using expressions, which are not allowed on message sources. "
                                          + "Offending parameters are: [%s]", model.getName(),
                                                                 Joiner.on(',').join(dynamicParams))));
  }

  public void setConfigurationProvider(ConfigurationProvider configurationProvider) {
    this.configurationProvider = configurationProvider;
  }

  public void setCursorProviderFactory(CursorProviderFactory cursorProviderFactory) {
    this.cursorProviderFactory = cursorProviderFactory;
  }

  public void setPrimaryNodeOnly(Boolean primaryNodeOnly) {
    this.primaryNodeOnly = primaryNodeOnly;
  }

  public void setBackPressureStrategy(BackPressureStrategy backPressureStrategy) {
    this.backPressureStrategy = backPressureStrategy;
  }
}
