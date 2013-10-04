/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.jboss.config;

import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.jboss.transaction.JBossArjunaTransactionManagerFactory;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers Bean Definition Parsers for the "jbossts" namespace.
 */
public class JbossTSNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("transaction-manager", new MuleOrphanDefinitionParser(JBossArjunaTransactionManagerFactory.class, true));
        registerBeanDefinitionParser("properties", new ChildMapEntryDefinitionParser("properties","key","value"));
    }

}
