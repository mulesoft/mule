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

import org.mule.config.i18n.CoreMessages;
import org.mule.impl.MuleDescriptor;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;

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
    public CommonsPoolProxyFactory(MuleDescriptor descriptor, UMOModel model)
    {
        super(descriptor, model);
    }

    public void activateObject(Object arg0) throws Exception
    {
        // nothing to do
    }

    public void destroyObject(Object object) throws Exception
    {
        pool.onRemove(object);
    }

    public Object makeObject() throws Exception
    {
        Object object = create();
        pool.onAdd(object);
        return object;
    }

    public void passivateObject(Object arg0) throws Exception
    {
        // nothing to do
    }

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
            throw new InitialisationException(
                CoreMessages.failedToSetPropertiesOn("Component '" + descriptor.getName() + "'"), 
                e, descriptor);
        }
    }

}
