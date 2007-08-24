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

import org.mule.providers.jdbc.JdbcTransactionFactory;
import org.mule.umo.UMOTransactionFactory;

import javax.sql.DataSource;

import org.enhydra.jdbc.standard.StandardDataSource;

public class JdbcTransactionalJdbcFunctionalTestCase extends AbstractJdbcTransactionalFunctionalTestCase
{

    protected UMOTransactionFactory getTransactionFactory()
    {
        return new JdbcTransactionFactory();
    }

    protected DataSource createDataSource() throws Exception
    {
        StandardDataSource ds = new StandardDataSource();
        ds.setDriverName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:.");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

}
