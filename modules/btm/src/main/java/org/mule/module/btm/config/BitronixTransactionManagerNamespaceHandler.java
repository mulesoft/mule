/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.config;

import org.mule.config.spring.parsers.generic.MuleOrphanDefinitionParser;
import org.mule.module.btm.transaction.BitronixTransactionManagerFactory;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Registers Bean Definition Parsers for the "jbossts" namespace.
 */
public class BitronixTransactionManagerNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("transaction-manager", new MuleOrphanDefinitionParser(BitronixTransactionManagerFactory.class, true));
    }

}
