/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transaction;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.transaction.TransactionManager;

import org.junit.Test;

public class TransactionManagerInjectTestCase extends AbstractIntegrationTestCase
{

    @Override
    protected String getConfigFile()
    {
        return "org/mule/test/integration/transaction/transaction-manager-inject.xml";
    }

    @Test
    public void injectTransactionManager()
    {
        TransactionClient txClient = (TransactionClient) muleContext.getRegistry().lookupObject("txClient");
        assertThat(txClient.getTxMgr(), not(nullValue()));
    }

    public static class TransactionClient
    {
        private TransactionManager txMgr;

        public TransactionManager getTxMgr()
        {
            return txMgr;
        }

        @Inject
        public void setTxMgr(TransactionManager txMgr)
        {
            this.txMgr = txMgr;
        }
    }
}
