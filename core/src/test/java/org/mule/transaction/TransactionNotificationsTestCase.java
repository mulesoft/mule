/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.transaction;

import org.mule.impl.internal.notifications.TransactionNotification;
import org.mule.impl.internal.notifications.TransactionNotificationListener;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.manager.UMOServerNotification;

import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class TransactionNotificationsTestCase extends AbstractMuleTestCase
{
    public void testTransactionNotifications() throws Exception
    {
        final CountDownLatch latch = new CountDownLatch(3);

        managementContext.registerListener(new TransactionNotificationListener()
        {
            public void onNotification(UMOServerNotification notification)
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
        });

        // the code is simple and deceptive :) The trick is this dummy transaction is handled by
        // a global TransactionCoordination instance, which binds it to the current thread.
        UMOTransaction transaction = new DummyTransaction();
        transaction.begin();
        transaction.commit();
        transaction.rollback();

        // Wait for the notifcation event to be fired as they are queued
        latch.await(2000, TimeUnit.MILLISECONDS);
        assertEquals("There are still some notifications left unfired.", 0, latch.getCount());
    }


    private class DummyTransaction extends AbstractTransaction
    {

        protected void doBegin() throws TransactionException
        {
            // nothing to do
        }

        protected void doCommit() throws TransactionException
        {
            // nothing to do
        }

        protected void doRollback() throws TransactionException
        {
            // nothing to do
        }

        public int getStatus() throws TransactionException
        {
            return 0;
        }

        public Object getResource(Object key)
        {
            return null;
        }

        public boolean hasResource(Object key)
        {
            return false;
        }

        public void bindResource(Object key, Object resource) throws TransactionException
        {
            // nothing to do
        }

        public void setRollbackOnly() throws TransactionException
        {
            // nothing to do
        }
    }

}
