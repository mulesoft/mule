/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.transaction.lookup.GenericTransactionManagerLookupFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.transaction.TransactionManager;

/**
 * <code>TestTransactionManagerFactory</code> TODO
 */
public class TestTransactionManagerFactory extends GenericTransactionManagerLookupFactory
{
    public TransactionManager create() throws Exception
    {
        return (TransactionManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                           new Class[] {TransactionManager.class},
                                                           new InternalInvocationHandler());
    }

    public void initialise()
    {

    }

    public class InternalInvocationHandler implements InvocationHandler
    {
        public TestTransactionManagerFactory getParent()
        {
            return TestTransactionManagerFactory.this;
        }

        public Object invoke (Object proxy, Method method, Object[] args) throws Throwable
        {
            return null;
        }

    }
}
