/*
 * $Header$
 * $Revision$
 * $Date$
 * ------------------------------------------------------------------------------------------------------
 *
 * Copyright (c) Cubis Limited. All rights reserved.
 * http://www.cubis.co.uk
 *
 * The software in this package is published under the terms of the BSD
 * style license a copy of which has been included with this distribution in
 * the LICENSE.txt file.
 */
package org.mule.providers.jdbc.functional;

import org.enhydra.jdbc.standard.StandardXADataSource;
import org.mule.transaction.XaTransactionFactory;
import org.mule.umo.UMOTransactionFactory;
import org.objectweb.jotm.Current;
import org.objectweb.jotm.Jotm;

/**
 * @author Guillaume Nodet
 * @version $Revision$
 */
public class JdbcTransactionalXaFunctionalTestCase extends AbstractJdbcTransactionalFunctionalTestCase {

    protected void setUp() throws Exception {
    	super.setUp();
		// check for already active JOTM instance
		Current jotmCurrent = Current.getCurrent();
		// if none found, create new local JOTM instance
		if (jotmCurrent == null) {
			new Jotm(true, false);
			jotmCurrent = Current.getCurrent();
		}
    	manager.setTransactionManager(jotmCurrent);
    }

    protected UMOTransactionFactory getTransactionFactory() {
        return new XaTransactionFactory();
    }
    
	protected Object createDataSource() throws Exception {
		StandardXADataSource ds = new StandardXADataSource();
		ds.setDriverName("org.hsqldb.jdbcDriver");
        ds.setUrl("jdbc:hsqldb:mem:.");
        ds.setUser("sa");
        ds.setPassword("");
    	return ds;
	}
	
	
}
