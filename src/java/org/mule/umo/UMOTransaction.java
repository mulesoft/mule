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

import org.mule.transaction.TransactionRollbackException;
import org.mule.transaction.TransactionStatusException;

/**
 * <p/>
 * <code>UMOTransaction</code> TODO (document class)
 *
 * @author <a href="mailto:ross.mason@cubis.co.uk">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOTransaction
{
    public void begin() throws UMOTransactionException;

    public void commit() throws UMOTransactionException;

    public void rollback() throws TransactionRollbackException;

    public int getStatus() throws TransactionStatusException;

    public abstract boolean isBegun() throws TransactionStatusException;

    public abstract boolean isRolledBack() throws TransactionStatusException;

    public abstract boolean isCommitted() throws TransactionStatusException;

    public Object getResource();

    public void setRollbackOnly();

    public boolean isRollbackOnly();
}
