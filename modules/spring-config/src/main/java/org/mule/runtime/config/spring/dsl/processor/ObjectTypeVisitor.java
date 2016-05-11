/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.processor;

import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.config.i18n.CoreMessages;

import org.apache.commons.lang.ClassUtils;

/**
 * Visitor that retrieves the {@code ComponentModel} object {@code Class} based
 * on the component configuration.
 *
 * @since 4.0
 */
public class ObjectTypeVisitor implements TypeDefinitionVisitor
{

    private final ComponentModel componentModel;
    private Class<?> type;

    public ObjectTypeVisitor(ComponentModel componentModel)
    {
        this.componentModel = componentModel;
    }

    @Override
    public void onType(Class<?> type)
    {
        this.type = type;
    }

    @Override
    public void onConfigurationAttribute(String attributeName)
    {
        try
        {
            type = ClassUtils.getClass(Thread.currentThread().getContextClassLoader(), componentModel.getParameters().get(attributeName));
        }
        catch (ClassNotFoundException e)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage("Error while trying to locate Class definition for type %s on element %s", componentModel.getParameters().get(attributeName), componentModel.getIdentifier()), e);
        }
    }

    public Class<?> getType()
    {
        return type;
    }
}
