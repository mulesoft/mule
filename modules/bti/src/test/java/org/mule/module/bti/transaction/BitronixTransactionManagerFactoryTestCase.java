/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.transaction;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.api.MuleContext;
import org.mule.api.config.MuleConfiguration;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;
import org.mule.util.UUID;
import org.mule.util.queue.TransactionalQueueManager;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.TransactionManager;

import bitronix.tm.TransactionManagerServices;
import bitronix.tm.internal.BitronixSystemException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.internal.stubbing.defaultanswers.Answers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

@SmallTest
public class BitronixTransactionManagerFactoryTestCase extends AbstractMuleTestCase
{

    private final MuleContext mockMuleContext = mock(MuleContext.class, Answers.RETURNS_DEEP_STUBS.get());
    private final MuleConfiguration mockMuleConfiguration = mock(MuleConfiguration.class);
    private final TransactionalQueueManager mockTransactionalQueueManager = mock(TransactionalQueueManager.class);
    private final List<BitronixTransactionManagerFactory> createdFactories = new ArrayList<BitronixTransactionManagerFactory>();

    @Before
    public void setUp()
    {
        when(mockMuleContext.getConfiguration()).thenReturn(mockMuleConfiguration);
        when(mockMuleContext.getQueueManager()).thenReturn(mockTransactionalQueueManager);
        when(mockMuleConfiguration.getWorkingDirectory()).thenReturn(".");
        when(mockMuleConfiguration.getId()).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return UUID.getUUID();
            }
        });
    }

    @After
    public void tearDown()
    {
        for (BitronixTransactionManagerFactory createdFactory : createdFactories)
        {
            createdFactory.dispose();
        }
        TransactionManagerServices.getTransactionManager().shutdown();
    }

    @Test
    public void createReturnsAlwaysTheSameInstance() throws Exception
    {
        BitronixTransactionManagerFactory bitronixTransactionManagerFactory = createTransactionFactory();
        TransactionManager transactionManager = bitronixTransactionManagerFactory.create(mockMuleConfiguration);
        assertThat(bitronixTransactionManagerFactory.create(mockMuleConfiguration), is(transactionManager));
    }

    @Test
    public void createInTwoDifferentInstancesReturnThenSameTransactionManager() throws Exception
    {
        TransactionManager tm1 = createTransactionManagerFromNewFactory();
        TransactionManager tm2 = createTransactionManagerFromNewFactory();
        assertThat(tm1, is(tm2));
    }

    /**
     * If transaction manager is shutdown then bitronix will throw a BitronixSystemException
     */
    @Test(expected = BitronixSystemException.class)
    public void disposeShutdownsTransactionManager() throws Exception
    {
        BitronixTransactionManagerFactory bitronixTransactionManagerFactory = createTransactionFactory();
        TransactionManager transactionManager = bitronixTransactionManagerFactory.create(mockMuleConfiguration);
        bitronixTransactionManagerFactory.dispose();
        transactionManager.begin();
    }

    @Test
    public void moreCreatesThanDisposeDonNotShutdownTransactionManager() throws Exception
    {
        TransactionManager transactionManager = createTransactionManagerFromNewFactory();
        BitronixTransactionManagerFactory transactionFactory = createTransactionFactory();
        transactionFactory.create(mockMuleConfiguration);
        transactionFactory.dispose();
        /*
         * If transaction manager shutdowns then begin would throw a BitronixSystemException.
         */
        transactionManager.begin();
        transactionManager.commit();
    }

    @Test(expected = BitronixSystemException.class)
    public void createSeveralFactoryAndDisposeAllOfThemShutdownsTransactionManager() throws Exception
    {
        List<BitronixTransactionManagerFactory> bitronixTransactionManagerFactories = new ArrayList<BitronixTransactionManagerFactory>();
        for (int i = 0; i < 5; i++)
        {
            BitronixTransactionManagerFactory transactionFactory = createTransactionFactory();
            bitronixTransactionManagerFactories.add(transactionFactory);
            transactionFactory.create(mockMuleConfiguration);
        }
        TransactionManager tm = bitronixTransactionManagerFactories.get(0).create(mockMuleConfiguration);
        for (BitronixTransactionManagerFactory bitronixTransactionManagerFactory : bitronixTransactionManagerFactories)
        {
            bitronixTransactionManagerFactory.dispose();
        }
        tm.begin();
    }

    private TransactionManager createTransactionManagerFromNewFactory() throws Exception
    {
        BitronixTransactionManagerFactory bitronixTransactionManagerFactory = createTransactionFactory();
        return bitronixTransactionManagerFactory.create(mockMuleConfiguration);
    }

    private BitronixTransactionManagerFactory createTransactionFactory()
    {
        BitronixTransactionManagerFactory bitronixTransactionManagerFactory = new BitronixTransactionManagerFactory();
        bitronixTransactionManagerFactory.setMuleContext(mockMuleContext);
        createdFactories.add(bitronixTransactionManagerFactory);
        return bitronixTransactionManagerFactory;
    }

}
