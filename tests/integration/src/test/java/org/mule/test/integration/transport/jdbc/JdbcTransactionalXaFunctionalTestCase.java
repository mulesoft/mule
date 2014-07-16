/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transport.jdbc;

import org.mule.api.transaction.TransactionFactory;
import org.mule.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.jdbc.xa.DataSourceWrapper;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.enhydra.jdbc.standard.StandardXADataSource;
import org.junit.Ignore;

@Ignore
public class JdbcTransactionalXaFunctionalTestCase extends AbstractJdbcTransactionalFunctionalTestCase
{
    private TransactionManager txManager;

    @Override
    protected void doSetUp() throws Exception
    {
        txManager = new JBossArjunaTransactionManagerFactory().create(muleContext.getConfiguration());
        super.doSetUp();
        muleContext.setTransactionManager(txManager);
    }

    @Override
    protected TransactionFactory getTransactionFactory()
    {
        return new XaTransactionFactory();
    }

    @Override
    protected DataSource createDataSource() throws Exception
    {
        StandardXADataSource ds = new StandardXADataSource();
        ds.setDriverName(EMBEDDED_DRIVER_NAME);
        ds.setUrl(EMBEDDED_CONNECTION_STRING);
        ds.setTransactionManager(txManager);
        return new DataSourceWrapper(ds);
    }
}
