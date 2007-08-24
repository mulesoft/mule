/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.integration.providers.jdbc;

import org.mule.providers.jdbc.xa.DataSourceWrapper;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

import org.enhydra.jdbc.standard.StandardXADataSource;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

public class JdbcTransactionalXaFunctionalTestCase extends AbstractJdbcTransactionalFunctionalTestCase
{

    private TransactionManager txManager;

    protected void doSetUp() throws Exception
    {
        // check for already active JOTM instance
        txManager = Current.getCurrent();
        // if none found, create new local JOTM instance
        if (txManager == null)
        {
            new Jotm(true, false);
            txManager = Current.getCurrent();
        }
        super.doSetUp();
       managementContext.setTransactionManager(txManager);
    }

    protected UMOTransactionFactory getTransactionFactory()
    {
        return new XaTransactionFactory();
    }

    protected DataSource createDataSource() throws Exception
    {
        StandardXADataSource ds = new StandardXADataSource();
        ds.setDriverName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:.");
        ds.setUser("sa");
        ds.setPassword("");
        ds.setTransactionManager(txManager);
        return new DataSourceWrapper(ds, txManager);
    }

}
