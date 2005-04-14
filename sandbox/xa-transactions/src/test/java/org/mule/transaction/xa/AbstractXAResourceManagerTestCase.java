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
 *
 */
package org.mule.transaction.xa;

import javax.transaction.Transaction;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAResource;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.mule.transaction.xa.AbstractXAResourceManager.AbstractSession;
import org.objectweb.jotm.Jotm;

/**
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class AbstractXAResourceManagerTestCase extends TestCase {

	
	private Jotm jotm;
	private TransactionManager tm;
	
	protected void setUp() throws Exception {
		jotm = new Jotm(true, false);
		tm = jotm.getTransactionManager();
	}
	
	protected void tearDown() throws Exception {
		jotm.stop();
		jotm = null;
		tm = null;
	}
	
	public void test1() throws Exception {
		TestXAResourceManager rm = new TestXAResourceManager();
		rm.start();
		AbstractSession s = rm.createSession();
		
		tm.begin();
		Transaction tx = tm.getTransaction();
		tx.enlistResource(s);
		
		tx.delistResource(s, XAResource.TMSUCCESS);
		tx.commit();
		
	}
	
	protected static class TestXAResourceManager extends AbstractXAResourceManager {

		private static Log logger = LogFactory.getLog(TestXAResourceManager.class); 
		
		public AbstractSession createSession() {
			return new AbstractSession();
		}
		
		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.AbstractResourceManager#getLogger()
		 */
		protected Log getLogger() {
			return logger;
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.AbstractResourceManager#createTransactionContext(java.lang.Object)
		 */
		protected AbstractTransactionContext createTransactionContext(Object session) {
			return new AbstractTransactionContext();
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.AbstractResourceManager#doBegin(org.mule.transaction.xa.AbstractTransactionContext)
		 */
		protected void doBegin(AbstractTransactionContext context) {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.AbstractResourceManager#doPrepare(org.mule.transaction.xa.AbstractTransactionContext)
		 */
		protected int doPrepare(AbstractTransactionContext context) {
			// TODO Auto-generated method stub
			return 0;
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.AbstractResourceManager#doCommit(org.mule.transaction.xa.AbstractTransactionContext)
		 */
		protected void doCommit(AbstractTransactionContext context) throws ResourceManagerException {
			// TODO Auto-generated method stub
			
		}

		/* (non-Javadoc)
		 * @see org.mule.transaction.xa.AbstractResourceManager#doRollback(org.mule.transaction.xa.AbstractTransactionContext)
		 */
		protected void doRollback(AbstractTransactionContext context) throws ResourceManagerException {
			// TODO Auto-generated method stub
			
		}
		
	}
}
