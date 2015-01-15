/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.transport.vm.config;

import org.mule.module.springconfig.factories.QueueProfileFactoryBean;
import org.mule.module.springconfig.handlers.AbstractMuleNamespaceHandler;
import org.mule.module.springconfig.parsers.generic.ChildDefinitionParser;
import org.mule.module.springconfig.parsers.specific.TransactionDefinitionParser;
import org.mule.endpoint.URIBuilder;
import org.mule.transport.vm.VMConnector;
import org.mule.transport.vm.VMTransactionFactory;

/**
 * Reigsters a Bean Definition Parser for handling <code><vm:connector></code> elements.
 *
 */
public class VmNamespaceHandler extends AbstractMuleNamespaceHandler
{
    @Override
    public void init()
    {
        registerStandardTransportEndpoints(VMConnector.VM, URIBuilder.PATH_ATTRIBUTES);
        registerConnectorDefinitionParser(VMConnector.class);
        registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfileFactoryBean.class));
        registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(VMTransactionFactory.class));

        // DEPRECATED. Use "queue-profile"
        registerBeanDefinitionParser("queueProfile", new ChildDefinitionParser("queueProfile", QueueProfileFactoryBean.class));
    }
}
