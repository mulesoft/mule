/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the MuleSource MPL
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.config.pool;

import org.mule.config.i18n.Message;
import org.mule.config.i18n.Messages;
import org.mule.impl.MuleDescriptor;
import org.mule.umo.lifecycle.InitialisationException;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.pool.PoolableObjectFactory;

/**
 * <code>CommonsPoolProxyFactory</code> is used to create MuleProxies for use in a
 * proxy pool. This is a Jakarta commons-pool implementation.
 */
public class CommonsPoolProxyFactory extends AbstractProxyFactory implements PoolableObjectFactory
{
    /**
     * Creates a pool factory using the descriptor as the basis for creating its
     * objects
     * 
     * @param descriptor the descriptor to use to construct a MuleProxy
     * @see MuleDescriptor
     */
    public CommonsPoolProxyFactory(MuleDescriptor descriptor)
    {
        super(descriptor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool.PoolableObjectFactory#activateObject(java.lang.Object)
     */
    public void activateObject(Object arg0) throws Exception
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool.PoolableObjectFactory#destroyObject(java.lang.Object)
     */
    public void destroyObject(Object object) throws Exception
    {
        pool.onRemove(object);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool.PoolableObjectFactory#makeObject()
     */
    public Object makeObject() throws Exception
    {
        Object object = create();
        pool.onAdd(object);
        return object;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool.PoolableObjectFactory#passivateObject(java.lang.Object)
     */
    public void passivateObject(Object arg0) throws Exception
    {
        // nothing to do
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.commons.pool.PoolableObjectFactory#validateObject(java.lang.Object)
     */
    public boolean validateObject(Object arg0)
    {
        return true;
    }

    protected void afterComponentCreate(Object component) throws InitialisationException
    {
        try
        {
            BeanUtils.populate(component, descriptor.getProperties());
        }
        catch (Exception e)
        {
            throw new InitialisationException(new Message(Messages.FAILED_TO_SET_PROPERTIES_ON_X,
                "Component '" + descriptor.getName() + "'"), e, descriptor);
        }
    }

}
