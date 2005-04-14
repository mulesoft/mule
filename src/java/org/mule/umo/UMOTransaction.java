/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) SymphonySoft Limited. All rights reserved. http://www.symphonysoft.com
 * 
 * The software in this package is published under the terms of the BSD style
 * license a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 *  
 */

package org.mule.umo;


/**
 * 
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @author Guillaume Nodet
 * @version $Revision$
 */
public interface UMOTransaction
{
	
    public static final int STATUS_ACTIVE = 0;
    public static final int STATUS_MARKED_ROLLBACK = 1;
    public static final int STATUS_PREPARED = 2;
    public static final int STATUS_COMMITTED = 3;
    public static final int STATUS_ROLLEDBACK = 4;
    public static final int STATUS_UNKNOWN = 5;
    public static final int STATUS_NO_TRANSACTION = 6;
    public static final int STATUS_PREPARING = 7;
    public static final int STATUS_COMMITTING = 8;
    public static final int STATUS_ROLLING_BACK = 9;

    /**
     * Begin the transaction.
     * @throws TransactionException
     */
    public void begin() throws TransactionException;

    /**
     * Commit the transaction
     * @throws TransactionException
     */
    public void commit() throws TransactionException;

    /**
     * Rollback the transaction
     * @throws TransactionException
     */
    public void rollback() throws TransactionException;

    public int getStatus() throws TransactionException;

    public abstract boolean isBegun() throws TransactionException;

    public abstract boolean isRolledBack() throws TransactionException;

    public abstract boolean isCommitted() throws TransactionException;

    public Object getResource(Object key);
    
    public boolean hasResource(Object key);
    
    public void bindResource(Object key, Object resource) throws TransactionException;

    public void setRollbackOnly() throws TransactionException;

    public boolean isRollbackOnly() throws TransactionException;
}
