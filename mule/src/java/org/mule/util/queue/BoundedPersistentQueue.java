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
package org.mule.util.queue;

import EDU.oswego.cs.dl.util.concurrent.BoundedBuffer;
import org.mule.umo.UMOEvent;
import org.mule.umo.lifecycle.Disposable;
import org.mule.umo.lifecycle.InitialisationException;

/**
 * <code>BoundedPersistentQueue</code> is a UMOEvent queue implementation that can automatically
 * persist events when they are queue.
 *
 * @see UMOEvent
 *
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */

public class BoundedPersistentQueue extends BoundedBuffer implements Disposable
{
    private PersistenceStrategy ps;

    private boolean deleteOnTake = true;

    public BoundedPersistentQueue(int i) throws IllegalArgumentException
    {
        super(i);
    }

    public BoundedPersistentQueue(int i, PersistenceStrategy ps, String name, boolean deleteOnTake) throws IllegalArgumentException, InitialisationException
    {
        super(i);
        this.ps = ps;
        this.deleteOnTake = deleteOnTake;
        if(ps!=null) ps.initialise(this, name);
    }

    public void put(Object o) throws InterruptedException
    {
        if(ps!= null) ps.store((UMOEvent)o);
        super.put(o);
    }

    public Object take() throws InterruptedException
    {
        Object o = super.take();
        if(deleteOnTake) remove((UMOEvent)o);
        return o;
    }

    public boolean remove(Object o) throws PersistentQueueException
    {
         if(ps!= null) {
             return ps.remove((UMOEvent)o);
         } else {
             return false;
         }
    }

    public boolean isDeleteOnTake()
    {
        return deleteOnTake;
    }

    public void setDeleteOnTake(boolean deleteOnTake)
    {
        this.deleteOnTake = deleteOnTake;
    }

    public PersistenceStrategy getPersistenceStrategy()
    {
        return ps;
    }

    public void dispose()
    {
        if(ps!=null) ps.dispose();
    }
}
