/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transport.vm.config;

import org.mule.config.spring.factories.QueueProfileFactoryBean;
import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.specific.TransactionDefinitionParser;
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
