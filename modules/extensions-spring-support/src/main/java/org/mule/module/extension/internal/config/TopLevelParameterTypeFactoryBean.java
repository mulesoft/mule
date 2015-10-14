/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extension.internal.config;

import static org.apache.commons.lang.StringUtils.EMPTY;
import static org.mule.module.extension.internal.util.MuleExtensionUtils.getInitialiserEvent;
import org.mule.RequestContext;
import org.mule.api.MuleContext;
import org.mule.api.MuleEvent;
import org.mule.extension.api.introspection.ConfigurationModel;
import org.mule.extension.api.introspection.DataType;
import org.mule.extension.api.introspection.OperationModel;
import org.mule.extension.api.introspection.ParameterModel;
import org.mule.module.extension.internal.runtime.resolver.ValueResolver;

import org.springframework.beans.factory.FactoryBean;

/**
 * A {@link FactoryBean} that creates instances of a type defined by a
 * {@link DataType}. It will be defined as a top level element and will
 * serve as a reusable {@link ParameterModel} of a {@link ConfigurationModel} or {@link OperationModel}
 *
 * @since 3.7.0
 */
final class TopLevelParameterTypeFactoryBean implements FactoryBean<Object>
{

    private final ValueResolver valueResolver;
    private final MuleContext muleContext;

    TopLevelParameterTypeFactoryBean(ElementDescriptor element, DataType dataType, MuleContext muleContext)
    {
        this.valueResolver = XmlExtensionParserUtils.parseElement(element, EMPTY, dataType, null);
        this.muleContext = muleContext;
    }

    @Override
    public Object getObject() throws Exception
    {
        return valueResolver.resolve(getEvent());
    }

    @Override
    public Class<?> getObjectType()
    {
        return Object.class;
    }

    @Override
    public boolean isSingleton()
    {
        return !valueResolver.isDynamic();
    }

    private MuleEvent getEvent()
    {
        MuleEvent event = RequestContext.getEvent();
        return event != null ? event : getInitialiserEvent(muleContext);
    }
}
