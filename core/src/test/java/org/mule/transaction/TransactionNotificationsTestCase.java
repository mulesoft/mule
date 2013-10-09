/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.transaction;

import org.mule.api.MuleContext;
import org.mule.api.context.notification.TransactionNotificationListener;
import org.mule.api.transaction.Transaction;
import org.mule.api.transaction.TransactionException;
import org.mule.context.notification.TransactionNotification;
import org.mule.tck.junit4.AbstractMuleContextTestCase;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TransactionNotificationsTestCase extends AbstractMuleContextTestCase
{
    @Test
    public void testTransactionNotifications() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(3);

        // the code is simple and deceptive :) The trick is this dummy transaction is handled by
        // a global TransactionCoordination instance, which binds it to the current thread.
        Transaction transaction = new DummyTransaction(muleContext);

        muleContext.registerListener(new TransactionNotificationListener<TransactionNotification>()
        {
            public void onNotification(TransactionNotification notification)
            {
                if (notification.getAction() == TransactionNotification.TRANSACTION_BEGAN)
                {
                    assertEquals("begin", notification.getActionName());
                    latch.countDown();
                }
                else
                {
                    if (notification.getAction() == TransactionNotification.TRANSACTION_COMMITTED)
                    {
                        assertEquals("commit", notification.getActionName());
                        latch.countDown();
                    }
                    else
                    {
                        if (notification.getAction() == TransactionNotification.TRANSACTION_ROLLEDBACK)
                        {
                            assertEquals("rollback", notification.getActionName());
                            latch.countDown();
                        }
                    }
                }
            }
        }, transaction.getId());


        transaction.begin();
        transaction.commit();
        transaction.rollback();

        // Wait for the notifcation event to be fired as they are queued
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals("There are still some notifications left unfired.", 0, latch.getCount());
    }

    private class DummyTransaction extends AbstractSingleResourceTransaction
    {

        private DummyTransaction(MuleContext muleContext)
        {
            super(muleContext);
        }

        @Override
        protected Class getResourceType()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        protected Class getKeyType()
        {
            return null;  //To change body of implemented methods use File | Settings | File Templates.
        }

        protected void doBegin() throws TransactionException
        {

        }

        protected void doCommit() throws TransactionException
        {

        }

        protected void doRollback() throws TransactionException
        {
            
        }
    }

}
