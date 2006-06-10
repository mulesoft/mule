/*
 * $Id$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */

package org.mule.tck.testmodels.mule;

import org.mule.umo.manager.UMOTransactionManagerFactory;

import javax.transaction.TransactionManager;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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
                                                           new Class[] { TransactionManager.class },
                                                           new InvocationHandler() {
                                                               public Object invoke(Object proxy,
                                                                                    Method method,
                                                                                    Object[] args) throws Throwable
                                                               {
                                                                   return null;
                                                               }

                                                           });
    }
}
