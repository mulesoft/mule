/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.core.api.config.ThreadingProfile.DEFAULT_THREADING_PROFILE;
import static org.mule.runtime.core.config.i18n.MessageFactory.createStaticMessage;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.ThreadingProfile;
import org.mule.runtime.core.api.retry.RetryPolicyTemplate;
import org.mule.runtime.core.config.ImmutableThreadingProfile;
import org.mule.extension.api.introspection.RuntimeExtensionModel;
import org.mule.extension.api.introspection.source.RuntimeSourceModel;
import org.mule.extension.api.introspection.source.SourceModel;
import org.mule.extension.api.runtime.source.Source;
import org.mule.extension.api.runtime.source.SourceFactory;
import org.mule.runtime.core.internal.connection.ConnectionManagerAdapter;
import org.mule.runtime.module.extension.internal.manager.ExtensionManagerAdapter;
import org.mule.runtime.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.runtime.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.runtime.module.extension.internal.runtime.source.SourceConfigurer;

import com.google.common.base.Joiner;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which yields instances of {@link ExtensionMessageSource}
 *
 * @since 4.0
 */
final class ExtensionMessageSourceFactoryBean extends ExtensionComponentFactoryBean<ExtensionMessageSource>
{

    private final ElementDescriptor element;
    private final RuntimeSourceModel sourceModel;
    private final String configurationProviderName;
    private final MuleContext muleContext;
    private RetryPolicyTemplate retryPolicyTemplate;

    @Inject
    private ConnectionManagerAdapter connectionManagerAdapter;

    ExtensionMessageSourceFactoryBean(ElementDescriptor element,
                                      RuntimeExtensionModel extensionModel,
                                      RuntimeSourceModel sourceModel,
                                      String configurationProviderName,
                                      MuleContext muleContext) throws ConfigurationException
    {
        this.element = element;
        this.extensionModel = extensionModel;
        this.sourceModel = sourceModel;
        this.configurationProviderName = configurationProviderName;
        this.muleContext = muleContext;
    }

    @Override
    public ExtensionMessageSource getObject() throws Exception
    {
        ResolverSet resolverSet = parserDelegate.getResolverSet(element, sourceModel.getParameterModels());
        if (resolverSet.isDynamic())
        {
            throw dynamicParameterException(resolverSet, sourceModel, element);
        }

        retryPolicyTemplate = parserDelegate.getInfrastructureParameter(RetryPolicyTemplate.class);
        if (retryPolicyTemplate == null)
        {
            retryPolicyTemplate = connectionManagerAdapter.getDefaultRetryPolicyTemplate();
        }

        ExtensionMessageSource messageSource = new ExtensionMessageSource(extensionModel,
                                                                          sourceModel,
                                                                          getSourceFactory(resolverSet),
                                                                          configurationProviderName,
                                                                          getThreadingProfile(),
                                                                          retryPolicyTemplate,
                                                                          (ExtensionManagerAdapter) muleContext.getExtensionManager());
        muleContext.getInjector().inject(messageSource);

        return messageSource;
    }

    private ThreadingProfile getThreadingProfile()
    {
        ThreadingProfile tp = new ImmutableThreadingProfile(DEFAULT_THREADING_PROFILE);
        tp.setMuleContext(muleContext);

        return tp;
    }

    @Override
    public Class<?> getObjectType()
    {
        return ExtensionMessageSource.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }

    private SourceFactory getSourceFactory(ResolverSet resolverSet)
    {
        return () -> {
            Source source = sourceModel.getSourceFactory().createSource();
            try
            {
                return new SourceConfigurer(sourceModel, resolverSet, muleContext).configure(source);
            }
            catch (Exception e)
            {
                throw new MuleRuntimeException(createStaticMessage(format("Could not create generator for source '%s' in flow '%s'",
                                                                          sourceModel.getName(), element.getSourceElement().getParentNode().getLocalName())));
            }
        };
    }

    private ConfigurationException dynamicParameterException(ResolverSet resolverSet, SourceModel model, ElementDescriptor element)
    {
        List<String> dynamicParams = resolverSet.getResolvers().entrySet().stream()
                .filter(entry -> entry.getValue().isDynamic())
                .map(entry -> entry.getKey().getName())
                .collect(toList());

        return new ConfigurationException(createStaticMessage(format("The '%s' message source on flow '%s' is using expressions, which are not allowed on message sources. " +
                                                                     "Offending parameters are: [%s]",
                                                                     model.getName(), element.getSourceElement().getParentNode().getLocalName(), Joiner.on(',').join(dynamicParams))));
    }

    public void setRetryPolicyTemplate(RetryPolicyTemplate retryPolicyTemplate)
    {
        this.retryPolicyTemplate = retryPolicyTemplate;
    }
}
