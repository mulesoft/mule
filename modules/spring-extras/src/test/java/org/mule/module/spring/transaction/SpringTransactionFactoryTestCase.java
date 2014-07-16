/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.spring.transaction;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.api.transaction.Transaction;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import org.junit.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

public class SpringTransactionFactoryTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testCommit() throws Exception
    {
        TransactionStatus mockTS = mock(TransactionStatus.class);

        PlatformTransactionManager mockPTM = mock(PlatformTransactionManager.class);
        when(mockPTM.getTransaction(any(TransactionDefinition.class))).thenReturn(mockTS);

        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager(mockPTM);

        Transaction tx = factory.beginTransaction(muleContext);
        tx.commit();

        verify(mockPTM).commit(mockTS);
    }

    @Test
    public void testRollback() throws Exception
    {
        TransactionStatus mockTS = mock(TransactionStatus.class);

        PlatformTransactionManager mockPTM = mock(PlatformTransactionManager.class);
        when(mockPTM.getTransaction(any(TransactionDefinition.class))).thenReturn(mockTS);

        SpringTransactionFactory factory = new SpringTransactionFactory();
        factory.setManager(mockPTM);

        Transaction tx = factory.beginTransaction(muleContext);
        tx.rollback();

        verify(mockPTM).rollback(mockTS);
        verify(mockTS).setRollbackOnly();
    }
}
