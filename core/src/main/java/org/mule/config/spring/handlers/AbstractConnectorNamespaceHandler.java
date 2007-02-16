/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.handlers;

import org.mule.config.ConfigurationException;

/**
 * TODO document
 *
 */
public abstract class AbstractConnectorNamespaceHandler extends AbstractNamespaceHandler {

    public AbstractConnectorNamespaceHandler(ClassLoader classLoader) throws ConfigurationException {
        super(classLoader);
//        registerBeanDefinitionParser("receiver-connection-strategy", new MuleNamespaceHandler.ConnectionStrategyDefinitionParser("receivererConnectionStrategy"));
//        registerBeanDefinitionParser("dispatcher-connection-strategy", new MuleNamespaceHandler.ConnectionStrategyDefinitionParser("dispatcherConnectionStrategy"));
//        registerBeanDefinitionParser("receiver-threading-profile", new MuleNamespaceHandler.ThreadingProfileDefinitionParser("receiverThreadingProfile"));
//        registerBeanDefinitionParser("dispatcher-threading-profile", new MuleNamespaceHandler.ThreadingProfileDefinitionParser("dispatcherThreadingProfile"));
//        registerBeanDefinitionParser("service-overrides", new MuleNamespaceHandler.ServiceOverridesDefinitionParser());
//        registerBeanDefinitionParser("exception-strategy", new MuleNamespaceHandler.ExceptionStrategyDefinitionParser());
    }


}
