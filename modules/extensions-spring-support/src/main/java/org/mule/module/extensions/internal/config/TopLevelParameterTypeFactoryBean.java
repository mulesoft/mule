/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.extensions.internal.config;

import static org.apache.commons.lang.StringUtils.EMPTY;
import org.mule.VoidMuleEvent;
import org.mule.extensions.introspection.DataType;
import org.mule.module.extensions.internal.runtime.resolver.ValueResolver;

import org.springframework.beans.factory.FactoryBean;

final class TopLevelParameterTypeFactoryBean implements FactoryBean<Object>
{

    private final ValueResolver valueResolver;

    TopLevelParameterTypeFactoryBean(ElementDescriptor element, DataType dataType)
    {
        this.valueResolver = XmlExtensionParserUtils.parseElement(element, EMPTY, dataType, null);
    }

    @Override
    public Object getObject() throws Exception
    {
        return valueResolver.resolve(VoidMuleEvent.getInstance());
    }

    @Override
    public Class<?> getObjectType()
    {
        return Object.class;
    }

    @Override
    public boolean isSingleton()
    {
        return true;
    }
}
