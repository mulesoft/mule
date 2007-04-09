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

import org.mule.config.PoolingProfile;
import org.mule.impl.MuleDescriptor;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.UMOModel;
import org.mule.umo.model.UMOPoolFactory;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * <code>CommonsPoolFactory</code> is a commons-pool pool implementation for mule.
 * this is the default implementation used if no other is configured.
 */
public class CommonsPoolFactory implements UMOPoolFactory
{
    public ObjectPool createPool(UMODescriptor descriptor, UMOModel model, ObjectFactory factory, PoolingProfile pp)
    {
        return new CommonsPoolProxyPool((MuleDescriptor) descriptor, model, factory, pp);
    }

    public ObjectPool createPool(UMODescriptor descriptor, UMOModel model, PoolingProfile pp)
    {
        return new CommonsPoolProxyPool((MuleDescriptor) descriptor, model, new CommonsPoolProxyFactory(
            (MuleDescriptor) descriptor, model), pp);
    }
}
