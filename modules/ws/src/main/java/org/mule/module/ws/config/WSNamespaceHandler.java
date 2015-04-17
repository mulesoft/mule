/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.ws.config;

import org.mule.config.spring.handlers.AbstractMuleNamespaceHandler;
import org.mule.config.spring.parsers.generic.ChildDefinitionParser;
import org.mule.config.spring.parsers.generic.OrphanDefinitionParser;
import org.mule.config.spring.parsers.specific.MessageProcessorDefinitionParser;
import org.mule.module.ws.config.spring.parsers.specific.WSProxyDefinitionParser;
import org.mule.module.ws.consumer.WSConsumer;
import org.mule.module.ws.consumer.WSConsumerConfig;
import org.mule.module.ws.security.WSSecurity;
import org.mule.module.ws.security.WssDecryptSecurityStrategy;
import org.mule.module.ws.security.WssEncryptSecurityStrategy;
import org.mule.module.ws.security.WssSignSecurityStrategy;
import org.mule.module.ws.security.WssTimestampSecurityStrategy;
import org.mule.module.ws.security.WssUsernameTokenSecurityStrategy;
import org.mule.module.ws.security.WssVerifySignatureSecurityStrategy;

/**
 * Registers a Bean Definition Parser for handling <code><ws:*></code> elements.
 */
public class WSNamespaceHandler extends AbstractMuleNamespaceHandler
{

    private static final String STRATEGY_PROPERTY = "strategy";

    public void init()
    {
        // Flow Constructs
        registerBeanDefinitionParser("proxy", new WSProxyDefinitionParser());
        registerBeanDefinitionParser("consumer-config", (OrphanDefinitionParser) new OrphanDefinitionParser(WSConsumerConfig.class, true).addReference("connectorConfig"));
        registerBeanDefinitionParser("consumer", new MessageProcessorDefinitionParser(WSConsumer.class));
        registerBeanDefinitionParser("security", new ChildDefinitionParser("security", WSSecurity.class));
        registerBeanDefinitionParser("wss-username-token", new ChildDefinitionParser(STRATEGY_PROPERTY, WssUsernameTokenSecurityStrategy.class));
        registerBeanDefinitionParser("wss-timestamp", new ChildDefinitionParser(STRATEGY_PROPERTY, WssTimestampSecurityStrategy.class));
        registerBeanDefinitionParser("wss-sign", new ChildDefinitionParser(STRATEGY_PROPERTY, WssSignSecurityStrategy.class));
        registerBeanDefinitionParser("wss-verify-signature", new ChildDefinitionParser(STRATEGY_PROPERTY, WssVerifySignatureSecurityStrategy.class));
        registerBeanDefinitionParser("wss-encrypt", new ChildDefinitionParser(STRATEGY_PROPERTY, WssEncryptSecurityStrategy.class));
        registerBeanDefinitionParser("wss-decrypt", new ChildDefinitionParser(STRATEGY_PROPERTY, WssDecryptSecurityStrategy.class));
    }
}
