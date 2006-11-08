/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.providers.gs.space;

import net.jini.core.entry.Entry;
import net.jini.core.lease.Lease;
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
import org.mule.util.ArrayUtils;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.client.ExternalEntry;
import com.j_spaces.core.client.FinderException;
import com.j_spaces.core.client.SpaceFinder;

import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;

/**
 * Represents a JavaSpace object. This is a wrapper to the underlying space. The
 * Space is created using the GigaSpaces API.
 */
public class GSSpace extends AbstractSpace
{
    private final IJSpace space;
    private final BlockingQueue queue = new LinkedBlockingQueue(1000);
    private final long lease;
    private Entry entryTemplate;
    private Entry snapshot;

    protected GSSpace(String spaceUrl, boolean enableMonitorEvents) throws FinderException
    {
        this(spaceUrl, enableMonitorEvents, Lease.FOREVER);
    }

    protected GSSpace(String spaceUrl, boolean enableMonitorEvents, long lease) throws FinderException
    {
        super(spaceUrl, enableMonitorEvents);
        this.lease = lease;
        this.space = (IJSpace)this.findSpace(spaceUrl);
    }

    protected JavaSpace findSpace(String spaceUrl) throws FinderException
    {
        logger.info("Connecting to space: " + spaceUrl);
        return (JavaSpace)SpaceFinder.find(spaceUrl);
    }

    public void doPut(Object value) throws UMOSpaceException
    {
        doPut(value, lease);
    }

    public void doPut(Object value, long lease) throws UMOSpaceException
    {
        try
        {
            Class valueClass = value.getClass();
            if (Entry.class.isAssignableFrom(valueClass))
            {
                space.write((Entry)value, getTransaction(), lease);
            }
            else if (valueClass.isArray())
            {
                Entry[] entryArr = (Entry[])ArrayUtils.toArrayOfComponentType((Object[])value, Entry.class);
                space.writeMultiple(entryArr, getTransaction(), lease);
            }
            else
            {
                space.write(new ExternalEntry(name, new Object[]{value}), getTransaction(), lease);
            }
        }
        catch (Exception e)
        {
            throw new GSSpaceException(e);
        }
    }

    public Object doTake() throws UMOSpaceException
    {
        return doTake(Long.MAX_VALUE);
    }

    public Object doTake(long timeout) throws UMOSpaceException
    {
        // TODO timeout is not used yet

        try
        {
            if (snapshot == null)
            {
                snapshot = space.snapshot(entryTemplate);
            }

            // try taking from Q
            Object retValue = null;
            while (retValue == null)
            {
                retValue = queue.poll();
                if (retValue != null)
                {
                    continue;
                }

                // try multiple
                Entry[] entries = space.takeMultiple(snapshot, getTransaction(), Integer.MAX_VALUE);
                if (entries != null && entries.length > 0)
                {
                    for (int i = 0; i < entries.length; i++)
                    {
                        queue.put(entries[i]);
                    }
                    continue;
                }

                // try for 5 secs
                Object entry = space.take(snapshot, getTransaction(), 5000);
                if (entry != null)
                {
                    queue.put(entry);
                }
            }
            return retValue;
        }
        catch (Exception e)
        {
            // TODO: hack, sleep 1 sec to allow GS cluster to come up
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e1)
            {
                e1.printStackTrace();
            }
            throw new GSSpaceException(e);
        }
    }

    public Object doTakeNoWait() throws UMOSpaceException
    {
        try
        {
            return space.takeIfExists(entryTemplate, getTransaction(), 1);
        }
        catch (Exception e)
        {
            throw new GSSpaceException(e);
        }
    }

    protected void doDispose()
    {
        // TODO: how do you release a space?
    }

    public int size()
    {
        return -1;
    }

    public void beginTransaction() throws UMOSpaceException
    {
        try
        {
            UMOTransaction tx = transactionFactory.beginTransaction();
            tx.bindResource(name, space);
        }
        catch (org.mule.umo.TransactionException e)
        {
            throw new SpaceTransactionException(e);
        }
    }

    public void commitTransaction() throws UMOSpaceException
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx == null)
        {
            throw new SpaceTransactionException(new TransactionNotInProgressException(new Message(
                Messages.TX_COMMIT_FAILED)));
        }
        try
        {
            tx.commit();
        }
        catch (org.mule.umo.TransactionException e)
        {
            throw new SpaceTransactionException(e);
        }
    }

    public void rollbackTransaction() throws UMOSpaceException
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx == null)
        {
            throw new SpaceTransactionException(new TransactionNotInProgressException(new Message(
                Messages.TX_COMMIT_FAILED)));
        }
        try
        {
            tx.rollback();
        }
        catch (org.mule.umo.TransactionException e)
        {
            throw new SpaceTransactionException(e);
        }
    }

    public JavaSpace getJavaSpace()
    {
        return space;
    }

    protected Transaction getTransaction()
    {
        UMOTransaction tx = TransactionCoordination.getInstance().getTransaction();
        if (tx != null)
        {
            return (Transaction)tx.getResource(space);
        }
        else
        {
            return null;
        }
    }

    public Entry getEntryTemplate()
    {
        return entryTemplate;
    }

    public void setEntryTemplate(Entry entryTemplate)
    {
        this.entryTemplate = entryTemplate;
        this.snapshot = null;
        if (logger.isInfoEnabled())
        {
            logger.info("Space: " + name + " is using receiver template: " + entryTemplate.toString());
        }
    }

}
