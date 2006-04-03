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
package org.mule.providers.gs.space;

import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;

import net.jini.core.entry.Entry;
import net.jini.core.transaction.Transaction;
import net.jini.space.JavaSpace;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.space.AbstractSpace;
import org.mule.impl.space.SpaceTransactionException;
import org.mule.transaction.TransactionCoordination;
import org.mule.transaction.TransactionNotInProgressException;
import org.mule.umo.UMOTransaction;
import org.mule.umo.space.UMOSpaceException;

/**
 * Represents a JavaSpace object. This is a wrapper to the underlying space. The Space is created using the
 * GigaSpaces API.
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class GSSpace extends AbstractSpace {

    private JavaSpace space;
    private Entry entryTemplate;

    public GSSpace(String spaceUrl) throws FinderException {
        super(spaceUrl);
        findSpace(spaceUrl);
    }
    public GSSpace(String spaceUrl, boolean enableMonitorEvents) throws FinderException {
        super(spaceUrl, enableMonitorEvents);
        findSpace(spaceUrl);
    }

    protected void findSpace(String spaceUrl) throws FinderException {
        logger.info("Connecting to space: " + spaceUrl);
	    space = (JavaSpace)SpaceFinder.find(spaceUrl);
    }

    public void doPut(Object value) throws UMOSpaceException {

        doPut(value, Long.MAX_VALUE);

    }

    public void doPut(Object value, long lease) throws UMOSpaceException {

        try {
            if(value instanceof Entry) {
                space.write((Entry)value, getTransaction(), lease);
            } else {
                space.write(new ExternalEntry(name, new Object[]{value}), getTransaction(), lease);

            }
        } catch (Exception e) {
            throw new GSSpaceException(e);
        }
    }

    public Object doTake() throws UMOSpaceException {
       return doTake(Long.MAX_VALUE);
    }

    public Object doTake(long timeout) throws UMOSpaceException {
        try {
              return space.take(entryTemplate, getTransaction(), timeout);
        } catch (Exception e) {
            throw new GSSpaceException(e);
        }
    }

    public Object doTakeNoWait() throws UMOSpaceException {
        try {
              return space.takeIfExists(entryTemplate, getTransaction(), 1);
        } catch (Exception e) {
            throw new GSSpaceException(e);
        }
    }

    protected void doDispose() {
        //How do you release a space?
    }

    public int size() {
        return -1;
    }

    public void beginTransaction() throws UMOSpaceException {

        try {
            UMOTransaction tx = transactionFactory.beginTransaction();
            tx.bindResource(name, space);
        } catch (org.mule.umo.TransactionException e) {
            throw new SpaceTransactionException(e);
        }

    }

    public void commitTransaction() throws UMOSpaceException {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if(tx==null) {
            throw new SpaceTransactionException(
                    new TransactionNotInProgressException(new Message(Messages.TX_COMMIT_FAILED)));
        }
        try {
            tx.commit();
        } catch (org.mule.umo.TransactionException e) {
            throw new SpaceTransactionException(e);
        }
    }

    public void rollbackTransaction() throws UMOSpaceException {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if(tx==null) {
            throw new SpaceTransactionException(
                    new TransactionNotInProgressException(new Message(Messages.TX_COMMIT_FAILED)));
        }
        try {
            tx.rollback();
        } catch (org.mule.umo.TransactionException e) {
            throw new SpaceTransactionException(e);
        }
    }

    public JavaSpace getJavaSpace() {
        return space;
    }

    protected Transaction getTransaction() {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if(tx!=null) {
            return (Transaction)tx.getResource(space);
        } else {
            return null;
        }
    }

    public Entry getEntryTemplate() {
        return entryTemplate;
    }

    public void setEntryTemplate(Entry entryTemplate) {
        this.entryTemplate = entryTemplate;
    }
}
