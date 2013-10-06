/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.xa;

import org.mule.util.xa.AbstractXAResourceManager;
import org.mule.util.xa.DefaultXASession;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.naming.NamingException;
import javax.naming.Reference;
import javax.transaction.xa.XAResource;

import bitronix.tm.internal.XAResourceHolderState;
import bitronix.tm.recovery.RecoveryException;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;
import bitronix.tm.resource.common.XAResourceProducer;
import bitronix.tm.resource.common.XAStatefulHolder;
import bitronix.tm.utils.Scheduler;

/**
 * Bitronix infrastructure classes for mule core queues XA resources.
 */
public class DefaultXaSessionResourceProducer extends ResourceBean implements XAResourceProducer
{

    private final String uniqueName;
    private final AbstractXAResourceManager xaResourceManager;
    private List<XAResource> xaResources = new ArrayList<XAResource>();
    private ReadWriteLock xaResourcesLock = new ReentrantReadWriteLock();

    public DefaultXaSessionResourceProducer(String uniqueName, AbstractXAResourceManager xaResourceManager)
    {
        this.uniqueName = uniqueName;
        this.xaResourceManager = xaResourceManager;
        setTwoPcOrderingPosition(Scheduler.ALWAYS_LAST_POSITION);
    }

    @Override
    public String getUniqueName()
    {
        return uniqueName;
    }

    @Override
    public XAResourceHolderState startRecovery() throws RecoveryException
    {
        return new XAResourceHolderState(new DefaultXaSessionResourceHolder(new DefaultXASession(xaResourceManager), this), this);
    }

    @Override
    public void endRecovery() throws RecoveryException
    {
    }

    @Override
    public void setFailed(boolean failed)
    {
    }

    @Override
    public XAResourceHolder findXAResourceHolder(XAResource xaResource)
    {
        Lock lock = xaResourcesLock.readLock();
        lock.lock();
        try
        {
            for (XAResource resource : xaResources)
            {
                if (resource == xaResource)
                {
                    return new DefaultXaSessionResourceHolder(resource, this);
                }
            }
            return null;
        }
        finally
        {
            lock.unlock();
        }
    }

    @Override
    public void init()
    {
    }

    @Override
    public void close()
    {
    }

    @Override
    public XAStatefulHolder createPooledConnection(Object xaFactory, ResourceBean bean) throws Exception
    {
        return null;
    }

    @Override
    public Reference getReference() throws NamingException
    {
        return null;
    }

    public void addDefaultXASession(XAResource session)
    {
        Lock lock = xaResourcesLock.writeLock();
        lock.lock();
        try
        {
            this.xaResources.add(session);
        }
        finally
        {
            lock.unlock();
        }
    }

    public void removeDefaultXASession(XAResource xaResource)
    {
        Lock lock = xaResourcesLock.writeLock();
        lock.lock();
        try
        {
            this.xaResources.remove(xaResource);
        }
        finally
        {
            lock.unlock();
        }
    }
}

