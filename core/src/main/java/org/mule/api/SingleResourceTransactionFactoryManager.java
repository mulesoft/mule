/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
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
