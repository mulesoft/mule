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
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getClassLoader;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.source.SourceCallbackModel;
import org.mule.runtime.api.meta.model.source.SourceModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.core.streaming.CursorProviderFactory;
import org.mule.runtime.extension.api.runtime.ConfigurationProvider;
import org.mule.runtime.extension.api.runtime.source.Source;
import org.mule.runtime.module.extension.internal.config.dsl.AbstractExtensionObjectFactory;
import org.mule.runtime.module.extension.internal.runtime.resolver.ParametersResolver;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapter;
import org.mule.runtime.module.extension.internal.runtime.source.SourceAdapterFactory;
import org.mule.runtime.module.extension.internal.runtime.source.SourceConfigurer;
import org.mule.runtime.module.extension.internal.util.MuleExtensionUtils;

import com.google.common.base.Joiner;

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

/**
 * An {@link AbstractExtensionObjectFactory} that produces instances of {@link ExtensionMessageSource}
 *
 * @since 4.0
 */
public class ExtensionSourceObjectFactory extends AbstractExtensionObjectFactory<ExtensionMessageSource> {

  private final ExtensionModel extensionModel;
  private final SourceModel sourceModel;

  private ConfigurationProvider configurationProvider;
  private RetryPolicyTemplate retryPolicyTemplate;
  private CursorProviderFactory cursorProviderFactory;

  @Inject
  private ConnectionManagerAdapter connectionManager;

  public ExtensionSourceObjectFactory(ExtensionModel extensionModel, SourceModel sourceModel, MuleContext muleContext) {
    super(muleContext);
    this.extensionModel = extensionModel;
    this.sourceModel = sourceModel;
  }

  protected ParametersResolver getParametersResolver(MuleContext muleContext) {
    return ParametersResolver.fromValues(parameters, muleContext);
  }

  @Override
  public ExtensionMessageSource doGetObject() throws ConfigurationException, InitialisationException {
    return withContextClassLoader(getClassLoader(extensionModel), () -> {
      parametersResolver.checkParameterGroupExclusiveness(sourceModel, parameters.keySet());
      ResolverSet nonCallbackParameters = getNonCallbackParameters();

      if (nonCallbackParameters.isDynamic()) {
        throw dynamicParameterException(nonCallbackParameters, sourceModel);
      }

      ResolverSet responseCallbackParameters = getCallbackParameters(sourceModel.getSuccessCallback());
      ResolverSet errorCallbackParameters = getCallbackParameters(sourceModel.getErrorCallback());
      ResolverSet terminateCallbackParameters = getCallbackParameters(sourceModel.getTerminateCallback());

      initialiseIfNeeded(nonCallbackParameters, true, muleContext);
      initialiseIfNeeded(responseCallbackParameters, true, muleContext);
      initialiseIfNeeded(errorCallbackParameters, true, muleContext);

      return new ExtensionMessageSource(extensionModel,
                                        sourceModel,
                                        getSourceFactory(nonCallbackParameters, responseCallbackParameters,
                                                         errorCallbackParameters, terminateCallbackParameters),
                                        configurationProvider,
                                        getRetryPolicyTemplate(),
                                        cursorProviderFactory,
                                        muleContext.getExtensionManager());
    });
  }

  private ResolverSet getNonCallbackParameters() throws ConfigurationException {
    return parametersResolver.getParametersAsResolverSet(sourceModel, sourceModel.getParameterGroupModels().stream()
        .flatMap(g -> g.getParameterModels().stream())
        .collect(toList()), muleContext);
  }

  private ResolverSet getCallbackParameters(Optional<SourceCallbackModel> callbackModel) throws ConfigurationException {
    if (callbackModel.isPresent()) {
      return parametersResolver.getParametersAsResolverSet(callbackModel.get(), muleContext);
    }

    return new ResolverSet(muleContext);
  }

  private SourceAdapterFactory getSourceFactory(ResolverSet nonCallbackParameters,
                                                ResolverSet successCallbackParameters,
                                                ResolverSet errorCallbackParameters,
                                                ResolverSet terminateCallbackParameters) {
    return (configurationInstance, sourceCallbackFactory) -> {
      Source source = MuleExtensionUtils.getSourceFactory(sourceModel).createSource();
      try {
        source = new SourceConfigurer(sourceModel, nonCallbackParameters, muleContext)
            .configure(source, configurationInstance);

        return new SourceAdapter(extensionModel,
                                 sourceModel,
                                 source,
                                 configurationInstance,
                                 cursorProviderFactory,
                                 sourceCallbackFactory,
                                 nonCallbackParameters,
                                 successCallbackParameters,
                                 terminateCallbackParameters,
                                 errorCallbackParameters);
      } catch (Exception e) {
        throw new MuleRuntimeException(createStaticMessage(format("Could not create generator for source '%s'",
                                                                  sourceModel.getName())),
                                       e);
      }
    };
  }

  private RetryPolicyTemplate getRetryPolicyTemplate() throws ConfigurationException {
    return retryPolicyTemplate != null ? retryPolicyTemplate : connectionManager.getDefaultRetryPolicyTemplate();
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
}
