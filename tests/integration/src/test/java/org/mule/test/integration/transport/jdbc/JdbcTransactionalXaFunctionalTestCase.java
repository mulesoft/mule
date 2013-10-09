/*
 * (c) 2003-2014 MuleSoft, Inc. This software is protected under international copyright
 * law. All use of this software is subject to MuleSoft's Master Subscription Agreement
 * (or other master license agreement) separately entered into in writing between you and
 * MuleSoft. If such an agreement is not in place, you may not use the software.
 */
package org.mule.test.integration.transport.jdbc;

import org.mule.api.transaction.TransactionFactory;
import org.mule.module.jboss.transaction.JBossArjunaTransactionManagerFactory;
import org.mule.transaction.XaTransactionFactory;
import org.mule.transport.jdbc.xa.DataSourceWrapper;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.enhydra.jdbc.standard.StandardXADataSource;


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
