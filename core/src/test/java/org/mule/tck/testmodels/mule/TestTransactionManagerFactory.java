/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.tck.testmodels.mule;

import org.mule.api.config.MuleConfiguration;
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
    public TransactionManager create(MuleConfiguration config) throws Exception
    {
        return (TransactionManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                           new Class[] {TransactionManager.class},
                                                           new InternalInvocationHandler());
    }

    public void initialise()
    {
        // shortcut super's implementation
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
