/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tck.testmodels.mule.TestTransactionManagerFactory;

import java.lang.reflect.Proxy;
import java.util.Map;

import javax.transaction.TransactionManager;

import org.junit.Test;

public class CustomTransactionManagerTestCase extends AbstractIntegrationTestCase
{
    @Override
    public String getConfigFile()
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
        TestTransactionManagerFactory factory = ihandler.getParent();
        Map<?, ?> properties = factory.getEnvironment();
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
