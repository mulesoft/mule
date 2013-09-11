/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.api;

import java.util.HashMap;
import java.util.Map;

import org.mule.api.transaction.TransactionFactory;
import org.mule.config.i18n.CoreMessages;

/**
 *
 */
public class SingleResourceTransactionFactoryManager
{
    private Map<Class,TransactionFactory> transactionFactories = new HashMap<Class, TransactionFactory>();
    private Map<Class,TransactionFactory> transactionFactoriesCache = new HashMap<Class, TransactionFactory>();

    public void registerTransactionFactory(Class supportedType, TransactionFactory transactionFactory)
    {
        this.transactionFactories.put(supportedType, transactionFactory);
    }
    
    public boolean supports(Class type)
    {
        return this.transactionFactories.containsKey(type);
    }
    
    public TransactionFactory getTransactionFactoryFor(Class type)
    {
        TransactionFactory transactionFactory = transactionFactoriesCache.get(type);
        if (transactionFactory == null)
        {
            for (Class transactionResourceType : transactionFactories.keySet())
            {
                if (transactionResourceType.isAssignableFrom(type))
                {
                    transactionFactory = transactionFactories.get(transactionResourceType);
                    this.transactionFactoriesCache.put(type, transactionFactory);
                    break;
                }
            }
        }
        if (transactionFactory == null)
        {
            throw new MuleRuntimeException(CoreMessages.createStaticMessage(String.format("No %s for transactional resource %s",TransactionFactory.class.getName(),type.getName())));
        }
        return transactionFactory;
    }
}
