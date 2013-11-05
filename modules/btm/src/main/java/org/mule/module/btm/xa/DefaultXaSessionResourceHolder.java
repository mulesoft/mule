/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.module.btm.xa;

import java.util.Date;
import java.util.List;

import javax.transaction.xa.XAResource;

import bitronix.tm.resource.common.AbstractXAResourceHolder;
import bitronix.tm.resource.common.ResourceBean;
import bitronix.tm.resource.common.XAResourceHolder;

/**
 * Bitronix infrastructure classes for mule core queues XA resources.
 *
 * No recovery needed for VM since it's always going to be managed as the last resource in the 2PC protocol.
 */
public class DefaultXaSessionResourceHolder extends AbstractXAResourceHolder
{

    private final XAResource xaResource;
    private final ResourceBean resourceBean;
    private int state;

    public DefaultXaSessionResourceHolder(XAResource xaResource, ResourceBean resourceBean)
    {
        this.xaResource = xaResource;
        this.resourceBean = resourceBean;
    }

    @Override
    public XAResource getXAResource()
    {
        return xaResource;
    }

    @Override
    public ResourceBean getResourceBean()
    {
        return resourceBean;
    }

    @Override
    public int getState()
    {
        return state;
    }

    @Override
    public void setState(int state)
    {
        this.state = state;
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