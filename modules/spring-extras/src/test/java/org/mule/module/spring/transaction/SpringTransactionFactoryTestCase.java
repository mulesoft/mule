/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.module.spring.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import com.mockobjects.dynamic.C;
import com.mockobjects.dynamic.Mock;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

public class SpringTransactionFactoryTestCase extends AbstractMuleContextTestCase
{

    @Test
    public void testCommit() throws Exception
    {
        Mock mockPTM = new Mock(PlatformTransactionManager.class);
        Mock mockTS = new Mock(TransactionStatus.class);
        mockPTM.expectAndReturn("getTransaction", C.same(null), mockTS.proxy());
        mockPTM.expect("commit", C.same(mockTS.proxy()));

        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager((PlatformTransactionManager)mockPTM.proxy());

        Transaction tx = factory.beginTransaction(muleContext);
//        TransactionCoordination.getInstance().bindTransaction(tx);
        tx.commit();
    }

    @Test
    public void testRollback() throws Exception
    {
        Mock mockPTM = new Mock(PlatformTransactionManager.class);
        Mock mockTS = new Mock(TransactionStatus.class);
        mockPTM.expectAndReturn("getTransaction", C.same(null), mockTS.proxy());
        mockPTM.expect("rollback", C.same(mockTS.proxy()));
        mockTS.expect("setRollbackOnly");

        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager((PlatformTransactionManager)mockPTM.proxy());

        Transaction tx = factory.beginTransaction(muleContext);
//        TransactionCoordination.getInstance().bindTransaction(tx);
        tx.rollback();
    }

}
