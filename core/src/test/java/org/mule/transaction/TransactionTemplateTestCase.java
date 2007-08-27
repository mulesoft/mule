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

import org.mule.impl.DefaultExceptionStrategy;
import org.mule.impl.MuleTransactionConfig;
import org.mule.tck.AbstractMuleTestCase;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionConfig;

import com.mockobjects.dynamic.Mock;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;

/**
 * http://mule.mulesource.org/jira/browse/MULE-1494
 */
public class TransactionTemplateTestCase extends AbstractMuleTestCase
{
    protected Mock mockTm = new Mock(TransactionManager.class);

    protected void doSetUp() throws Exception
    {
        TransactionManager tm = (TransactionManager) mockTm.proxy();
        managementContext.setTransactionManager(tm);
    }

    public void testNoNestedTxStarted() throws Exception
    {
        Mock mockTx = new Mock(Transaction.class);
        mockTm.expect("begin");
        mockTm.expectAndReturn("getTransaction", mockTx.proxy());
        // anything which is not rolled/ing back is fine
        mockTx.expectAndReturn("getStatus", UMOTransaction.STATUS_ACTIVE);

        // and the final commit expectations
        mockTx.expectAndReturn("getStatus", UMOTransaction.STATUS_ACTIVE);
        mockTx.expect("commit");

        // this is one component with a TX always begin
        UMOTransactionConfig config = new MuleTransactionConfig();
        config.setFactory(new XaTransactionFactory());
        config.setAction(UMOTransactionConfig.ACTION_ALWAYS_BEGIN);
        TransactionTemplate template = new TransactionTemplate(config, new DefaultExceptionStrategy(), managementContext);

        // and the callee component which should join the current XA transaction, not begin a nested one
        final UMOTransactionConfig nestedConfig = new MuleTransactionConfig();
        nestedConfig.setFactory(new XaTransactionFactory());
        nestedConfig.setAction(UMOTransactionConfig.ACTION_BEGIN_OR_JOIN);

        // start the call chain
        template.execute(new TransactionCallback()
        {
            public Object doInTransaction() throws Exception
            {
                // the callee executes within its own TX template, but uses the same global XA transaction,
                // bound to the current thread of execution via a ThreadLocal
                TransactionTemplate nestedTemplate =
                        new TransactionTemplate(nestedConfig, new DefaultExceptionStrategy(), managementContext);
                return nestedTemplate.execute(new TransactionCallback()
                {
                    public Object doInTransaction() throws Exception
                    {
                        // do not care about the return really
                        return null;
                    }
                });
            }
        });

        // nobody leaves until checked ;)
        mockTm.verify();
        mockTx.verify();
    }

}
