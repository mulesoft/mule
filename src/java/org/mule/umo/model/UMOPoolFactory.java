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

package org.mule.umo.model;

import org.mule.umo.UMODescriptor;
import org.mule.util.ObjectFactory;
import org.mule.util.ObjectPool;

/**
 * <code>UMOPoolFactory</code> is a factory interface for created a component pool instance
 * 
 * @author <a href="mailto:ross.mason@symphonysoft.com">Ross Mason</a>
 * @version $Revision$
 */
public interface UMOPoolFactory
{
    ObjectPool createPool(UMODescriptor descriptor, ObjectFactory factory);

    ObjectPool createPool(UMODescriptor descriptor);
}
