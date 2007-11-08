/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.extras.jotm.config;

import org.mule.config.spring.parsers.specific.TransactionManagerDefinitionParser;
import org.mule.extras.jotm.JotmTransactionManagerFactory;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

public class JotmNamespaceHandler extends NamespaceHandlerSupport
{

    public void init()
    {
        registerBeanDefinitionParser("transaction-manager", new TransactionManagerDefinitionParser(JotmTransactionManagerFactory.class));
    }

}
