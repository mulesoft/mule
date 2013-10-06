/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.xa;

import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.transaction.xa.XAResource;

import bitronix.tm.BitronixXid;
import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.StateChangeListener;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.utils.Uid;

/**
 * Bitronix infrastructure classes for mule core queues XA resources.
 */
public class DefaultXaSessionResourceHolder implements XAResourceHolder
{

    private final XAResource xaResource;
    private final DefaultXaSessionResourceProducer defaultXaSessionResourceProducer;

    public DefaultXaSessionResourceHolder(XAResource xaResource, DefaultXaSessionResourceProducer defaultXaSessionResourceProducer)
    {
        this.xaResource = xaResource;
        this.defaultXaSessionResourceProducer = defaultXaSessionResourceProducer;
    }

    @Override
    public XAResource getXAResource()
    {
        return xaResource;
    }

    @Override
    public Map<Uid, XAResourceHolderState> getXAResourceHolderStatesForGtrid(Uid gtrid)
    {
        return null;
    }

    @Override
    public void putXAResourceHolderState(BitronixXid xid, XAResourceHolderState xaResourceHolderState)
    {
    }

    @Override
    public void removeXAResourceHolderState(BitronixXid xid)
    {
    }

    @Override
    public boolean hasStateForXAResource(XAResourceHolder xaResourceHolder)
    {
        return false;
    }

    @Override
    public ResourceBean getResourceBean()
    {
        return defaultXaSessionResourceProducer;
    }

    @Override
    public int getState()
    {
        return 0;
    }

    @Override
    public void setState(int state)
    {
    }

    @Override
    public void addStateChangeEventListener(StateChangeListener listener)
    {
    }

    @Override
    public void removeStateChangeEventListener(StateChangeListener listener)
    {
    }

    @Override
    public List<XAResourceHolder> getXAResourceHolders()
    {
        return null;
    }

    @Override
    public Object getConnectionHandle() throws Exception
    {
        return null;
    }

    @Override
    public void close() throws Exception
    {
    }

    @Override
    public Date getLastReleaseDate()
    {
        return null;
    }
}