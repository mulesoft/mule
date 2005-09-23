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
package org.mule.extras.spring.transaction;

import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;

import org.mule.transaction.AbstractSingleResourceTransaction;
import org.mule.umo.TransactionException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.UMOTransactionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * TODO: document this class
 * 
 * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
 * @version $Revision$
 */
public class SpringTransactionFactory implements UMOTransactionFactory
{

    /**
     * TODO: document this class
     * 
     * @author <a href="mailto:gnt@codehaus.org">Guillaume Nodet</a>
     */
    public class SpringTransaction extends AbstractSingleResourceTransaction
    {

        protected TransactionStatus status;
        protected AtomicBoolean started = new AtomicBoolean(false);
        protected AtomicBoolean committed = new AtomicBoolean(false);
        protected AtomicBoolean rolledBack = new AtomicBoolean(false);
        protected AtomicBoolean rollbackOnly = new AtomicBoolean(false);

        public SpringTransaction()
        {
            status = manager.getTransaction(null);
        }

        protected void doBegin() throws TransactionException
        {
        }

        protected void doCommit() throws TransactionException
        {
            manager.commit(status);
        }

        protected void doRollback() throws TransactionException
        {
            manager.rollback(status);
        }

        public Object getResource(Object key)
        {
            Object res = TransactionSynchronizationManager.getResource(key);
            if (res != null) {
                if (res instanceof org.springframework.jdbc.datasource.ConnectionHolder) {
                    return ((org.springframework.jdbc.datasource.ConnectionHolder) res).getConnection();
                }
                if (res instanceof org.springframework.jms.connection.ConnectionHolder) {
                    return ((org.springframework.jms.connection.ConnectionHolder) res).getConnection();
                }
            }
            return res;
        }

        public boolean hasResource(Object key)
        {
            return getResource(key) != null;
        }

        public void bindResource(Object key, Object resource) throws TransactionException
        {
            throw new UnsupportedOperationException();
        }

        public void setRollbackOnly()
        {
            super.setRollbackOnly();
            status.setRollbackOnly();
        }

    }

    private PlatformTransactionManager manager;

    public SpringTransactionFactory()
    {
    }

    public UMOTransaction beginTransaction() throws TransactionException
    {
        return new SpringTransaction();
    }

    public boolean isTransacted()
    {
        return true;
    }

    /**
     * @return Returns the manager.
     */
    public PlatformTransactionManager getManager()
    {
        return manager;
    }

    /**
     * @param manager The manager to set.
     */
    public void setManager(PlatformTransactionManager manager)
    {
        this.manager = manager;
    }

}
