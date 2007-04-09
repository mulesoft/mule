/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.umo.manager.UMOTransactionManagerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.transaction.TransactionManager;

/**
 * <code>TestTransactionManagerFactory</code> TODO
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class TestTransactionManagerFactory implements UMOTransactionManagerFactory
{
    public TransactionManager create() throws Exception
    {
        return (TransactionManager) Proxy.newProxyInstance(getClass().getClassLoader(),
                                                           new Class[] {TransactionManager.class},
                                                           new InvocationHandler()
        {
            public Object invoke (Object proxy, Method method, Object[] args) throws Throwable
            {
                return null;
            }

        });
    }
}
