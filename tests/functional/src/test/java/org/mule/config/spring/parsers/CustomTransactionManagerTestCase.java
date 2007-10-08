/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.config.spring.parsers;

import org.mule.tck.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.transaction.TransactionManager;

public class CustomTransactionManagerTestCase extends FunctionalTestCase
{
    public String getConfigResources()
    {
        return "test-custom-transaction-manager.xml";
    }

    public void testCustomTransactionManager() throws Exception
    {
        TransactionManager transactionManager = managementContext.getTransactionManager();
        assertTrue(transactionManager instanceof Proxy);
        Proxy proxy = (Proxy) transactionManager;
        TestTransactionManagerFactory.InternalInvocationHandler ihandler =
                (TestTransactionManagerFactory.InternalInvocationHandler) Proxy.getInvocationHandler(proxy);
        assertTrue(ihandler.getParent() instanceof TestTransactionManagerFactory);
        TestTransactionManagerFactory factory = ihandler.getParent();
        Map properties = factory.getEnvironment();
        assertEquals(properties.size(), 2);
        assertEquals(properties.get("property1"), "true");
        assertEquals(properties.get("property2"), "Test");
    }

/*
    public void testWeblogicTransactionManager() throws Exception
    {
        TransactionManager transactionManager = managementContext.getTransactionManager();
        assertNotNull(transactionManager);
        transactionManager.begin();
        Transaction transaction = transactionManager.getTransaction();
        assertNotNull(transaction);
        transactionManager.rollback();
        assertNull(transactionManager.getTransaction());
    }
*/
}
