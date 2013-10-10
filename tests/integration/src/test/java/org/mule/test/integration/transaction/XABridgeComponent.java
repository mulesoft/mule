/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transaction;

import org.mule.api.transaction.Transaction;
import org.mule.transaction.TransactionCoordination;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple service that receives messages from jdbc or jms and just forward the
 * interesting part.
 */
public class XABridgeComponent
{
    private static Log log = LogFactory.getLog(XABridgeComponent.class);

    public static boolean mayRollback = false;

    /**
     * If <code>mayRollback</code> has been set to true, the service will mark
     * the current transaction as rollback only on a 30 percent basis.
     * 
     * @throws Exception
     */
    protected void mayRollback() throws Exception
    {
        if (mayRollback)
        {
            Transaction tx = TransactionCoordination.getInstance().getTransaction();
            if (tx != null)
            {
                if (Math.random() < 0.3)
                {
                    log.info("Marking transaction for rollback");
                    tx.setRollbackOnly();
                }
            }
        }
    }

    /**
     * Receive the jdbc message and forward the <code>data</code> part. May mark
     * the current transaction as rollback only.
     * 
     * @param msg
     * @throws Exception
     */
    public Object onJdbcMessage(Map msg) throws Exception
    {
        mayRollback();
        return msg.get("data").toString();
    }

    /**
     * Receive the content of the jms message and forward it. May mark the current
     * transaction as rollback only.
     * 
     * @param msg
     * @throws Exception
     */
    public Object onJmsMessage(String msg) throws Exception
    {
        mayRollback();
        return msg;
    }

}
