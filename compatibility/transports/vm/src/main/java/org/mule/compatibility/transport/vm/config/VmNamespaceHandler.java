/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.compatibility.transport.vm.config;

import org.mule.compatibility.config.spring.handlers.AbstractMuleTransportsNamespaceHandler;
import org.mule.compatibility.core.endpoint.URIBuilder;
import org.mule.compatibility.transport.vm.VMConnector;
import org.mule.compatibility.transport.vm.VMTransactionFactory;
import org.mule.runtime.config.spring.factories.QueueProfileFactoryBean;
import org.mule.runtime.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.runtime.config.spring.parsers.specific.TransactionDefinitionParser;

/**
 * Reigsters a Bean Definition Parser for handling <code><vm:connector></code> elements.
 *
 */
public class VmNamespaceHandler extends AbstractMuleTransportsNamespaceHandler {

  @Override
  public void init() {
    registerStandardTransportEndpoints(VMConnector.VM, URIBuilder.PATH_ATTRIBUTES);
    registerConnectorDefinitionParser(VMConnector.class);
    registerBeanDefinitionParser("queue-profile", new ChildDefinitionParser("queueProfile", QueueProfileFactoryBean.class));
    registerBeanDefinitionParser("transaction", new TransactionDefinitionParser(VMTransactionFactory.class));

    // DEPRECATED. Use "queue-profile"
    registerBeanDefinitionParser("queueProfile", new ChildDefinitionParser("queueProfile", QueueProfileFactoryBean.class));
  }
}
