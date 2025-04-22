/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config.dsl.source;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.config.internal.dsl.utils.DslConstants.CONFIG_ATTRIBUTE_NAME;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy.WAIT;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.util.FunctionalUtils.withNullEvent;
import static org.mule.runtime.extension.api.ExtensionConstants.PRIMARY_NODE_ONLY_PARAMETER_NAME;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.toBackPressureStrategy;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.api.notification.NotificationDispatcher;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.policy.RetryPolicyTemplate;
import org.mule.runtime.core.api.source.MessageSource.BackPressureStrategy;
import org.mule.runtime.core.api.streaming.CursorProviderFactory;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.extension.api.property.BackPressureStrategyModelProperty;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;
import org.mule.runtime.module.extension.api.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapterFactory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.Joiner;

import org.slf4j.Logger;


/**
 * An {@link AbstractExtensionObjectFactory} that produces instances of {@link ExtensionMessageSource}
 *
 * @since 4.0
 */
public class ExtensionSourceObjectFactory extends AbstractExtensionObjectFactory<ExtensionMessageSource> {

  private final Logger LOGGER = getLogger(ExtensionSourceObjectFactory.class);

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;

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
      ResolverSet nonCallbackParameters = getNonCallbackParameters();

      if (hasDynamicNonCallbackParameters(nonCallbackParameters)) {
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
                                        getConfigurationProvider(),
                                        calculatePrimaryNodeOnly(nonCallbackParameters),
                                        getRetryPolicyTemplate(),
                                        cursorProviderFactory,
                                        backPressureStrategy,
                                        muleContext.getExtensionManager(),
                                        ((MuleContextWithRegistry) muleContext).getRegistry()
                                            .lookupObject(NotificationDispatcher.class),
                                        muleContext.getConfiguration().getId());
    });
  }

  private Boolean calculatePrimaryNodeOnly(ResolverSet nonCallbackParameters) {
    if (sourceModel.runsOnPrimaryNodeOnly()) {
      return true;
    }
    ValueResolver<?> primaryNodeOnlyValueResolver = nonCallbackParameters.getResolvers().get(PRIMARY_NODE_ONLY_PARAMETER_NAME);

    if (primaryNodeOnlyValueResolver != null) {
      return withNullEvent(event -> {
        try (ValueResolvingContext ctx = ValueResolvingContext.builder(event).build()) {
          return (Boolean) primaryNodeOnlyValueResolver.resolve(ctx);
        } catch (Exception e) {
          String errorMessage = format("There was a problem resolving the value of the %s parameter for the %s source.",
                                       PRIMARY_NODE_ONLY_PARAMETER_NAME, sourceModel.getName());
          LOGGER.error(errorMessage);
          throw new MuleRuntimeException(createStaticMessage(errorMessage), e);
        }
      });
    } else {
      return false;
    }
  }

  // TODO(MULE-15641): REMOVE THIS METHOD. REPLACE WITH `nonCallbackParameters.isDynamic()`
  private boolean hasDynamicNonCallbackParameters(ResolverSet nonCallbackParameters) {
    return nonCallbackParameters.getResolvers()
        .entrySet().stream()
        .filter(e -> !CONFIG_ATTRIBUTE_NAME.equals(e.getKey()))
        .anyMatch(e -> e.getValue().isDynamic());
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

    return new ResolverSet(muleContext.getInjector());
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
                                    expressionManager,
                                    muleContext);
  }

  private RetryPolicyTemplate getRetryPolicyTemplate() {
    return retryPolicyTemplate;
  }

  private ConfigurationProvider getConfigurationProvider() {
    return parameters.values().stream()
        .filter(ConfigurationProvider.class::isInstance)
        .map(v -> ((ConfigurationProvider) v)).findAny()
        .orElse(null);
  }

  public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate) {
    this.retryPolicyTemplate = retryPolicyTemplate;
  }

  private ConfigurationException dynamicParameterException(ResolverSet resolverSet, SourceModel model) {
    List<String> dynamicParams = resolverSet.getResolvers().entrySet()
        .stream()
        .filter(entry -> entry.getValue().isDynamic())
        .map(Map.Entry::getKey).collect(toList());

    String message = "The source: '" + model.getName() + "' is using expressions, which are not allowed on message sources."
        + " Offending parameters are: [" + Joiner.on(',').join(dynamicParams) + "]";

    return new ConfigurationException(createStaticMessage(message));
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
