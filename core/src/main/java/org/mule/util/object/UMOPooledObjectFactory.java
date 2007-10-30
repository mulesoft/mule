/*
 * $Id$
 * --------------------------------------------------------------------------------------
 * Copyright (c) MuleSource, Inc.  All rights reserved.  http://www.mulesource.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.util.object;

import org.mule.config.PoolingProfile;

public interface UMOPooledObjectFactory extends ObjectFactory
{
    public abstract int getPoolSize();

    public abstract PoolingProfile getPoolingProfile();

    public abstract void setPoolingProfile(PoolingProfile poolingProfile);

}
