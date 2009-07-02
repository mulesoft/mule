/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.factories;

import org.mule.api.MuleContext;
import org.mule.api.context.MuleContextAware;

import javax.transaction.TransactionManager;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.AbstractFactoryBean;

/**
 * Creates a reference to the TransactionManager configured on the MuleContext.  This is useful when you need to inject
 * the TransactionManager into other objects such as XA data Sources.
 *
 * In order to use this factory bean you must have a transaction manager configured in the context i.e.
 * <code><jbossts:transaction-manager/></code>
 */
public class MuleTransactionManagerFactoryBean extends AbstractFactoryBean implements MuleContextAware
{
    private MuleContext muleContext;

    public void setMuleContext(MuleContext context)
    {
        muleContext = context;
    }

    public Class getObjectType()
    {
        return TransactionManager.class;
    }

    protected Object createInstance() throws Exception
    {
        if(muleContext.getTransactionManager()==null)
        {
            throw new BeanCreationException("you must have a transaction manager configured inside the context when using " + getClass().getName());
        }
        return muleContext.getTransactionManager();
    }
}
