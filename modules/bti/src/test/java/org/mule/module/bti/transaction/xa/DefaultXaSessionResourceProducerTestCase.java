/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.bti.transaction.xa;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.module.bti.transaction.TransactionManagerWrapper;
import org.mule.module.bti.xa.DefaultXaSessionResourceProducer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;

public class DefaultXaSessionResourceProducerTestCase extends AbstractMuleTestCase
{

    private DefaultXaSessionResourceProducer xaSessionProducer;
    private TransactionManagerWrapper transactionManagerWrapper;
    private final XAResource firstXaResource = mock(DefaultXASession.class);
    private final XAResource secondXaResource = mock(DefaultXASession.class);

    @Before
    public void setUp()
    {
        xaSessionProducer = new DefaultXaSessionResourceProducer("uniqueName",
                                                                 mock(AbstractXAResourceManager.class));
        transactionManagerWrapper = new TransactionManagerWrapper(mock(TransactionManager.class, Answers.RETURNS_DEEP_STUBS.get()),
                                                                  xaSessionProducer);
    }

    @Test
    public void oneDefaultXaSessionEnlistedIsRemoved() throws Exception
    {
        Transaction transaction = transactionManagerWrapper.getTransaction();
        transactionManagerWrapper.begin();
        transaction.enlistResource(firstXaResource);
        assertThat(xaSessionProducer.findXAResourceHolder(firstXaResource).getXAResource(), is(xaSessionProducer.findXAResourceHolder(firstXaResource).getXAResource()));
        transactionManagerWrapper.rollback();
        assertThat(xaSessionProducer.findXAResourceHolder(firstXaResource), nullValue());
    }

    @Test
    public void severalDefaultXaSessionsEnlistedAreRemoved() throws Exception
    {
        Transaction transaction = transactionManagerWrapper.getTransaction();
        transactionManagerWrapper.begin();
        transaction.enlistResource(firstXaResource);
        transaction.enlistResource(secondXaResource);
        assertThat(xaSessionProducer.findXAResourceHolder(firstXaResource).getXAResource(), is(xaSessionProducer.findXAResourceHolder(firstXaResource).getXAResource()));
        assertThat(xaSessionProducer.findXAResourceHolder(firstXaResource).getXAResource(), not(is(xaSessionProducer.findXAResourceHolder(secondXaResource).getXAResource())));
        transactionManagerWrapper.rollback();
        assertThat(xaSessionProducer.findXAResourceHolder(firstXaResource), nullValue());
        assertThat(xaSessionProducer.findXAResourceHolder(secondXaResource), nullValue());
    }
}
