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
import org.mule.umo.lifecycle.InitialisationException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ObjectFactory backed by a Commons ObjectPool.  The characteristics of the ObjectPool can be 
 * customized by setting a PoolingProfile.
 */
public abstract class AbstractPooledObjectFactory extends AbstractObjectFactory implements UMOPooledObjectFactory 
{
    protected PoolingProfile poolingProfile = null;
    
    /** For Spring only */
    public AbstractPooledObjectFactory() { super(); }
    
    public AbstractPooledObjectFactory(Class objectClass) { super(objectClass); }

    public AbstractPooledObjectFactory(Class objectClass, Map properties) { super(objectClass, properties); }
    
    public AbstractPooledObjectFactory(Class objectClass, PoolingProfile poolingProfile) 
    { 
        super(objectClass);
        this.poolingProfile = poolingProfile;
    }

    public AbstractPooledObjectFactory(Class objectClass, Map properties, PoolingProfile poolingProfile) 
    { 
        super(objectClass, properties); 
        this.poolingProfile = poolingProfile;
    }
    
    public void initialise() throws InitialisationException
    {
        initialisePool();
        try
        {
            applyInitialisationPolicy();
        }
        catch (Exception e)
        {
            throw new InitialisationException(e, this);
        }
    }

    protected abstract void initialisePool();
    
    protected void applyInitialisationPolicy() throws Exception
    {
        if (poolingProfile != null)
        {
            int numToBorrow = 0;
            int initPolicy = poolingProfile.getInitialisationPolicy();
            if (initPolicy == PoolingProfile.INITIALISE_ALL)
            {
                numToBorrow = poolingProfile.getMaxActive();
            }
            else if (initPolicy == PoolingProfile.INITIALISE_ONE)
            {
                numToBorrow = 1;
            }
                
            List holderList = new ArrayList(numToBorrow);    
            try
            {
                for (int t = 0; t < numToBorrow; t++)
                {
                    holderList.add(this.getOrCreate());
                }
            }
            finally
            {
                for (int t = 0; t < holderList.size(); t++)
                {
                    Object obj = holderList.get(t);
                    if (obj != null)
                    {
                        this.release(obj);
                    }
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.mule.util.object.UMOPooledObjectFactory#getPoolSize()
     */
    public abstract int getPoolSize();
    
    ////////////////////////////////////////////////////////////////////////////////////////////////
    // Getters and Setters
    ////////////////////////////////////////////////////////////////////////////////////////////////

    /* (non-Javadoc)
     * @see org.mule.util.object.UMOPooledObjectFactory#getPoolingProfile()
     */
    public PoolingProfile getPoolingProfile()
    {
        return poolingProfile;
    }

    /* (non-Javadoc)
     * @see org.mule.util.object.UMOPooledObjectFactory#setPoolingProfile(org.mule.config.PoolingProfile)
     */
    public void setPoolingProfile(PoolingProfile poolingProfile)
    {
        this.poolingProfile = poolingProfile;
    }
}
