/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TimeFactoryBean;

import org.springframework.beans.factory.xml.NamespaceHandler;

/**
 * A {@link NamespaceHandler} for elements that configure how the platform
 * handles extensions
 *
 * @since 4.0
 */
public class ExtensionConfigNamespaceHandler extends AbstractMuleNamespaceHandler
{

    @Override
    public void init()
    {
        registerBeanDefinitionParser("extensions-config", new ChildDefinitionParser("extension", DefaultExtensionConfig.class));
        registerBeanDefinitionParser("dynamic-configuration-expiration", new ChildDefinitionParser("dynamicConfigExpirationFrequency", TimeFactoryBean.class));
    }
}
