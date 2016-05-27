/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.transport.socket.internal.config;

import org.mule.runtime.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.runtime.config.spring.parsers.delegate.RootOrNestedElementBeanDefinitionParser;
import org.mule.runtime.transport.socket.internal.DefaultTcpClientSocketProperties;
import org.mule.runtime.transport.socket.internal.DefaultTcpServerSocketProperties;

public class SocketsNamespaceHandler extends AbstractMuleNamespaceHandler
{

    public void init()
    {
        registerBeanDefinitionParser("client-socket-properties", new RootOrNestedElementBeanDefinitionParser(DefaultTcpClientSocketProperties.class, "clientSocketProperties"));
        registerBeanDefinitionParser("server-socket-properties",  new RootOrNestedElementBeanDefinitionParser(DefaultTcpServerSocketProperties.class, "serverSocketProperties"));
    }

}
