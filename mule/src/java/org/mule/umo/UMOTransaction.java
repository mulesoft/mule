/*
 * $Header$ $Revision$ $Date$
 * ------------------------------------------------------------------------------------------------------
 * 
 * Copyright (c) Cubis Limited. All rights reserved. http://www.cubis.co.uk
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
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
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
     * @throws UMOTransactionException
     */
    public void begin() throws UMOTransactionException;

    /**
     * Commit the transaction
     * @throws UMOTransactionException
     */
    public void commit() throws UMOTransactionException;

    /**
     * Rollback the transaction
     * @throws UMOTransactionException
     */
    public void rollback() throws UMOTransactionException;

    public int getStatus() throws UMOTransactionException;

    public abstract boolean isBegun() throws UMOTransactionException;

    public abstract boolean isRolledBack() throws UMOTransactionException;

    public abstract boolean isCommitted() throws UMOTransactionException;

    public Object getResource(Object key);
    
    public boolean hasResource(Object key);
    
    public void bindResource(Object key, Object resource) throws UMOTransactionException;

    public void setRollbackOnly() throws UMOTransactionException;

    public boolean isRollbackOnly() throws UMOTransactionException;
}
