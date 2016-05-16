/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.runtime.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.RequestContext;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.extension.api.introspection.config.ConfigurationModel;
import org.mule.runtime.extension.api.introspection.operation.OperationModel;
import org.mule.runtime.extension.api.introspection.parameter.ParameterModel;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that creates instances of a type defined by a
 * {@link MetadataType}. It will be defined as a top level element and will
 * serve as a reusable {@link ParameterModel} of a {@link ConfigurationModel} or {@link OperationModel}
 *
 * @since 3.7.0
 */
final class TopLevelParameterTypeFactoryBean extends ExtensionComponentFactoryBean<Object>
{

    private final ElementDescriptor element;
    private final MetadataType metadataType;
    private final MuleContext muleContext;
    private ValueResolver<Object> valueResolver;

    TopLevelParameterTypeFactoryBean(ElementDescriptor element, MetadataType metadataType, MuleContext muleContext)
    {
        this.element = element;
        this.metadataType = metadataType;
        this.muleContext = muleContext;
    }

    @Override
    public Object getObject() throws Exception
    {
        return getValueResolver().resolve(getEvent());
    }

    @Override
    public Class<?> getObjectType()
    {
        return Object.class;
    }

    @Override
    public boolean isSingleton()
    {
        return !getValueResolver().isDynamic();
    }

    private ValueResolver<Object> getValueResolver()
    {
        if (valueResolver == null)
        {
            valueResolver = parserDelegate.parseElement(new ParameterParsingDescriptor(element, EMPTY, EMPTY, metadataType, null));
        }

        return valueResolver;
    }

    private MuleEvent getEvent()
    {
        MuleEvent event = RequestContext.getEvent();
        return event != null ? event : getInitialiserEvent(muleContext);
    }
}
