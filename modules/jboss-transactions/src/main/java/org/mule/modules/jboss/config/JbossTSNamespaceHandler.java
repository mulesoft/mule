/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.modules.jboss.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.config.spring.parsers.collection.ChildMapEntryDefinitionParser;
import org.mule.modules.jboss.transactions.JBossArjunaTransactionManagerFactory;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers Bean Definition Parsers for the "jbossts" namespace.
 */
public class JbossTSNamespaceHandler extends NamespaceHandlerSupport
{
    public void init()
    {
        registerBeanDefinitionParser("jboss-transaction-manager", new MuleOrphanDefinitionParser(JBossArjunaTransactionManagerFactory.class, true));
        registerBeanDefinitionParser("properties", new ChildMapEntryDefinitionParser("properties","key","value"));
    }

}
