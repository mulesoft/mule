/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
