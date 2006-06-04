/*
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

    int STATUS_ACTIVE = 0;
    int STATUS_MARKED_ROLLBACK = 1;
    int STATUS_PREPARED = 2;
    int STATUS_COMMITTED = 3;
    int STATUS_ROLLEDBACK = 4;
    int STATUS_UNKNOWN = 5;
    int STATUS_NO_TRANSACTION = 6;
    int STATUS_PREPARING = 7;
    int STATUS_COMMITTING = 8;
    int STATUS_ROLLING_BACK = 9;

    /**
     * Begin the transaction.
     * 
     * @throws TransactionException
     */
    void begin() throws TransactionException;

    /**
     * Commit the transaction
     * 
     * @throws TransactionException
     */
    void commit() throws TransactionException;

    /**
     * Rollback the transaction
     * 
     * @throws TransactionException
     */
    void rollback() throws TransactionException;

    int getStatus() throws TransactionException;

    boolean isBegun() throws TransactionException;

    boolean isRolledBack() throws TransactionException;

    boolean isCommitted() throws TransactionException;

    Object getResource(Object key);

    boolean hasResource(Object key);

    void bindResource(Object key, Object resource) throws TransactionException;

    void setRollbackOnly() throws TransactionException;

    boolean isRollbackOnly() throws TransactionException;
}
