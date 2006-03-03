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
package org.mule.config.pool;

import org.mule.impl.MuleDescriptor;
import org.mule.umo.UMODescriptor;
import org.mule.umo.model.UMOPoolFactory;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * <code>CommonsPoolFactory</code> is a commons-pool pool implementation for
 * mule. this is the default implementation used if no other is configured.
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public class CommonsPoolFactory implements UMOPoolFactory
{
    public ObjectPool createPool(UMODescriptor descriptor, ObjectFactory factory)
    {
        return new CommonsPoolProxyPool((MuleDescriptor) descriptor, factory);
    }

     public ObjectPool createPool(UMODescriptor descriptor)
    {
        return new CommonsPoolProxyPool((MuleDescriptor) descriptor, new CommonsPoolProxyFactory((MuleDescriptor)descriptor));
    }
}
