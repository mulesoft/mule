/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.extension.api.introspection.RuntimeExtensionModel;
import org.mule.runtime.extension.api.introspection.property.SubTypesModelProperty;
import org.mule.runtime.module.extension.internal.introspection.SubTypesMappingContainer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.xml.BeanDefinitionParser;

/**
 * Base class for all {@link FactoryBean}s used to parse extension components.
 * <p>
 * It contains a {@link XmlExtensionParserDelegate} which is injected by
 * the {@link BeanDefinitionParser} and is used to do actual parsing
 * with the correct context and relevant state.
 *
 * @param <T> the generic type of the objects to be produced
 * @since 4.0
 */
abstract class ExtensionComponentFactoryBean<T> implements FactoryBean<T>, HasExtensionParserDelegate
{

    protected RuntimeExtensionModel extensionModel;
    protected XmlExtensionParserDelegate parserDelegate;

    public void setParserDelegate(XmlExtensionParserDelegate parserDelegate)
    {
        this.parserDelegate = parserDelegate;

        if (extensionModel != null && extensionModel.getModelProperty(SubTypesModelProperty.class).isPresent())
        {
            this.parserDelegate.setSubTypesMapping(
                    new SubTypesMappingContainer(extensionModel.getModelProperty(SubTypesModelProperty.class).get().getSubTypesMapping()));
        }
    }

}
