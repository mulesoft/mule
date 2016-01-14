/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static org.mule.api.config.ThreadingProfile.DEFAULT_THREADING_PROFILE;
import static org.mule.config.i18n.MessageFactory.createStaticMessage;
import static org.mule.module.extension.internal.config.XmlExtensionParserUtils.getResolverSet;
import org.mule.api.MuleContext;
import org.mule.api.MuleRuntimeException;
import org.mule.api.config.ConfigurationException;
import org.mule.api.config.ThreadingProfile;
import org.mule.config.ImmutableThreadingProfile;
import org.mule.extension.api.introspection.ExtensionModel;
import org.mule.extension.api.introspection.SourceModel;
import org.mule.extension.api.runtime.source.Source;
import org.mule.extension.api.runtime.source.SourceFactory;
import org.mule.module.extension.internal.runtime.resolver.ResolverSet;
import org.mule.module.extension.internal.runtime.source.ExtensionMessageSource;
import org.mule.module.extension.internal.runtime.source.SourceConfigurer;

import com.google.common.base.Joiner;

import java.util.List;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} which yields instances of {@link ExtensionMessageSource}
 *
 * @since 4.0
 */
final class ExtensionMessageSourceFactoryBean implements FactoryBean<ExtensionMessageSource>
{

    private final ElementDescriptor element;
    private final ExtensionModel extensionModel;
    private final SourceModel sourceModel;
    private final String configurationProviderName;
    private final MuleContext muleContext;
    private final ResolverSet resolverSet;

    ExtensionMessageSourceFactoryBean(ElementDescriptor element,
                                      ExtensionModel extensionModel,
                                      SourceModel sourceModel,
                                      String configurationProviderName,
                                      MuleContext muleContext) throws ConfigurationException
    {
        this.element = element;
        this.extensionModel = extensionModel;
        this.sourceModel = sourceModel;
        this.configurationProviderName = configurationProviderName;
        this.muleContext = muleContext;

        resolverSet = getResolverSet(element, sourceModel.getParameterModels());
        if (resolverSet.isDynamic())
        {
            throw dynamicParameterException(resolverSet, sourceModel, element);
        }
    }

    @Override
    public ExtensionMessageSource getObject() throws Exception
    {
        ExtensionMessageSource messageSource = new ExtensionMessageSource(extensionModel, getSourceFactory(), configurationProviderName, getThreadingProfile());
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

    private SourceFactory getSourceFactory()
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
                                                                          sourceModel.getName(), element.getParentNode().getLocalName())));
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
                                                                     model.getName(), element.getParentNode().getLocalName(), Joiner.on(',').join(dynamicParams))));
    }
}
