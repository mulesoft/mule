/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) SymphonySoft Limited. All rights reserved.
 * http://www.symphonysoft.com
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.test.integration.providers.jdbc;

import org.enhydra.jdbc.standard.StandardXADataSource;
import org.mule.providers.jdbc.xa.DataSourceWrapper;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

import javax.sql.DataSource;
import javax.transaction.TransactionManager;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcTransactionalXaFunctionalTestCase extends AbstractJdbcTransactionalFunctionalTestCase {

	private TransactionManager txManager;
	
    protected void setUp() throws Exception {
		// check for already active JOTM instance
    	txManager = Current.getCurrent();
		// if none found, create new local JOTM instance
		if (txManager == null) {
			new Jotm(true, false);
			txManager = Current.getCurrent();
		}
    	super.setUp();
    	manager.setTransactionManager(txManager);
    }

    protected UMOTransactionFactory getTransactionFactory() {
        return new XaTransactionFactory();
    }
    
	protected DataSource createDataSource() throws Exception {
		StandardXADataSource ds = new StandardXADataSource();
		ds.setDriverName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:.");
        ds.setUser("sa");
        ds.setPassword("");
        ds.setTransactionManager(txManager);
        return new DataSourceWrapper(ds, txManager);
	}
	
	
}
