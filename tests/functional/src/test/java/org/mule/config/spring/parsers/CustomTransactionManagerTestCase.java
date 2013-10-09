/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.config.spring.parsers;

import org.mule.tck.junit4.FunctionalTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CustomTransactionManagerTestCase extends FunctionalTestCase
{

    @Override
    public String getConfigResources()
    {
        return "test-custom-transaction-manager.xml";
    }

    @Test
    public void testCustomTransactionManager() throws Exception
    {
        TransactionManager transactionManager = muleContext.getTransactionManager();
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
     * Attention: this test only runs successful when it's the only one. As soon
     * as the test above is added, muleContext contains more than one transaction
     * manager and all kinds of havoc happen here.

    @Test
    public void testWeblogicTransactionManager() throws Exception
    {
        TransactionManager transactionManager = muleContext.getTransactionManager();
        assertNotNull(transactionManager);
        transactionManager.begin();
        Transaction transaction = transactionManager.getTransaction();
        assertNotNull(transaction);
        transactionManager.rollback();
        assertNull(transactionManager.getTransaction());
    }
    */

}
